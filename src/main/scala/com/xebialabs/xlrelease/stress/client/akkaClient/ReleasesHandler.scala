package com.xebialabs.xlrelease.stress.client.akkaClient

import akka.stream.Materializer
import akka.util.Timeout
import cats.implicits._
import cats.syntax._
import com.xebialabs.xlrelease.stress.client.Releases
import com.xebialabs.xlrelease.stress.client.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.domain.Release.ID
import com.xebialabs.xlrelease.stress.domain.Team.{releaseAdmin, templateOwner}
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.domain.User.Session
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class ReleasesHandler(val client: AkkaHttpXlrClient)(implicit val ec: ExecutionContext, m: Materializer) {

  implicit def releasesHandler: Releases.Handler[Future] = new Releases.Handler[Future] with DefaultJsonProtocol {
    protected def importTemplate(session: User.Session, owner: User, template: Template): Future[Template.ID] =
      for {
        templateId <- importTemplate0(template)(session)
        _ <- client.setTemplateTeams(templateId, List(templateOwner(owner, templateId), releaseAdmin(owner, templateId)))(session)
      } yield templateId

    protected def setTemplateTeam(session: Session, templateId: Template.ID, teams: Set[Team]): Future[Map[String, String]] =
      client.setTemplateTeams(templateId, teams.toList)(session)
        .asJson.map {
        case JsArray(arr) =>
          arr.toList.collect {
            case team: JsObject =>
              team.getFields("teamName", "id") match {
                case Seq(JsString(teamName), JsString(id)) => Option(teamName -> id)
                case _ => None
              }
            case _ => None
          }
        case _ => List.empty
      }.map { teamIds: List[Option[(String, String)]] =>
        teamIds
          .sequence
          .map(_.toMap)
          .getOrElse(Map.empty)
      }

    protected def setTemplateScriptUser(session: Session, templateId: ID, scriptUser: Option[User]): Future[Unit] =
      client.setTemplateScriptUser(templateId, scriptUser.getOrElse(session.user))(session).discardU

    protected def create(session: User.Session, templateId: Template.ID, createReleaseArgs: CreateReleaseArgs): Future[Release.ID] =
      client.createRelease(templateId, createReleaseArgs)(session)
        .asJson
        .flatMap {
          case JsObject(r) => r.get("id") match {
            case Some(JsString(id)) => Future.successful(id.replaceFirst("Applications/", ""))
            case _ => Future.failed(new RuntimeException("Cannot extract Release ID"))
          }
          case _ => Future.failed(new RuntimeException("not a Js object"))
        }

    protected def start(session: Session, releaseId: Release.ID): Future[Release.ID] = {
      for {
        _ <- Future.successful {
          println(s"Starting release: $releaseId")
        }
        releaseId <- client.startRelease(releaseId)(session)
          .discard(_ => releaseId)
      } yield releaseId
    }

    protected def getTasksByTitle(session: Session, releaseId: Release.ID, taskTitle: String, phaseTitle: Option[String]): Future[Set[Task.ID]] =
      client.getTaskByTitle(releaseId, taskTitle, phaseTitle)(session).collect {
        case JsArray(tasks) => tasks.toList.collect {
          case task: JsObject =>
            task.fields.get("id").flatMap {
              case JsString(fullId) => fullId.split("/").toList match {
                case "Applications" :: `releaseId` :: phaseId :: Nil =>
                  println("No task id!!")
                  None
                case "Applications" :: `releaseId` :: phaseId :: taskId =>
                  Some(Task.ID(Phase.ID(releaseId, phaseId), taskId.mkString("/")))
                case otherId =>
                  println("Task Id does not match Applications/releaseId/phaseId/...")
                  None
              }
              case _ => None
            }
        }.sequence.map(_.toSet).getOrElse(Set.empty)
      }

    override protected def waitFor(session: Session, releaseId: ID, expectedStatus: ReleaseStatus): Future[Unit] = {
      implicit val timeout: Timeout = Timeout(10 seconds)

      getReleaseStatus(releaseId)(session)
        .until(_.contains(expectedStatus), interval = 5 seconds, retries = Some(20))
    }
  }

  def importTemplate0(template: Template)(implicit session: User.Session): Future[Release.ID] = {
    client.importTemplate(template)
      .asJson
      .map[Option[String]] {
      case JsArray(ids) =>
        ids.headOption.flatMap(_.asJsObject.fields.get("id")).flatMap {
          case JsString(id) => Some(id)
          case _ => Option.empty[Template.ID]
        }
      case _ => Option.empty[Template.ID]
    }.flatMap(_.fold(Future.failed[Template.ID](new RuntimeException("Cannot extract Template ID")))(Future.successful))
  }

  def getReleaseStatus(releaseId: Release.ID)(implicit session: User.Session): () => Future[Option[ReleaseStatus]] =
    () => client.getRelease(releaseId)
      .map(readReleaseStatus)


  def readReleaseStatus: JsValue => Option[ReleaseStatus] = {
    case JsObject(fields) =>
      fields.get("status").map(_.convertTo[ReleaseStatus])
    case _ => None
  }

  def matchesReleaseStatus(expectedStatus: ReleaseStatus): JsValue => Boolean = {
    case JsArray(arr) =>
      arr.headOption
        .flatMap(readReleaseStatus)
        .contains(expectedStatus)
    case _ => false
  }

}

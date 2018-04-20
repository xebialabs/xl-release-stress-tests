package com.xebialabs.xlrelease.stress.handlers.xlr.akkaClient

import akka.stream.Materializer
import akka.util.Timeout
import cats.effect.IO
import cats.implicits._
import com.xebialabs.xlrelease.stress.api.xlr.Releases
import com.xebialabs.xlrelease.stress.api.xlr.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.domain.Release.ID
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.domain.User.Session
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class ReleasesHandler(implicit val client: AkkaHttpXlrClient, ec: ExecutionContext, m: Materializer) {

  implicit def releasesHandler: Releases.Handler[IO] = new Releases.Handler[IO] with DefaultJsonProtocol {
    protected def importTemplate(session: User.Session, template: Template): IO[Template.ID] =
      client.importTemplate(template)(session)
        .asJson
        .io
        .map[Option[String]] {
        case JsArray(ids) =>
          ids.headOption.flatMap(_.asJsObject.fields.get("id")).flatMap {
            case JsString(id) => Some(id)
            case _ => Option.empty[Template.ID]
          }
        case _ => Option.empty[Template.ID]
      }.flatMap(_.fold(IO.raiseError[Template.ID](new RuntimeException("Cannot extract Template ID")))(IO.pure))

    protected def setTemplateTeams(session: Session, templateId: Template.ID, teams: Seq[Team]): IO[Map[String, String]] =
      client.setTemplateTeams(templateId, teams.toList)(session)
        .asJson
        .io
        .map {
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

    protected def setTemplateScriptUser(session: Session, templateId: ID, scriptUser: Option[User]): IO[Unit] =
      client.setTemplateScriptUser(templateId, scriptUser.getOrElse(session.user))(session)
        .discardU
        .io

    protected def create(session: User.Session, templateId: Template.ID, createReleaseArgs: CreateReleaseArgs): IO[Release.ID] =
      client.createRelease(templateId, createReleaseArgs)(session)
        .asJson
        .io
        .flatMap {
          case JsObject(r) => r.get("id") match {
            case Some(JsString(id)) => IO.pure(id.replaceFirst("Applications/", ""))
            case _ => IO.raiseError(new RuntimeException("Cannot extract Release ID"))
          }
          case _ => IO.raiseError(new RuntimeException("not a Js object"))
        }

    protected def start(session: Session, releaseId: Release.ID): IO[Release.ID] =
      client.startRelease(releaseId)(session)
        .discard(_ => releaseId)
        .io

    protected def getTasksByTitle(session: Session, releaseId: Release.ID, taskTitle: String, phaseTitle: Option[String]): IO[Set[Task.ID]] =
      client.getTaskByTitle(releaseId, taskTitle, phaseTitle)(session).io.map {
        case JsArray(tasks) => tasks.toList.collect {
          case JsObject(fields) =>
            fields.get("id").flatMap {
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
          case _ => None
        }.sequence.map(_.toSet).getOrElse(Set.empty)
        case _ => Set.empty
      }

    override protected def waitFor(session: Session, releaseId: ID, expectedStatus: ReleaseStatus,
                                   interval: Duration = 5 seconds, retries: Option[Int] = Some(20)): IO[Unit] = {
      implicit val timeout: Timeout = Timeout(10 seconds)

      getReleaseStatus(releaseId)(session)
        .until(_.contains(expectedStatus), interval, retries)
        .io
    }
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

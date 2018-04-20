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
    protected def importTemplate(template: Template)
                                (implicit session: User.Session): IO[Template.ID] =
      client.importTemplate(template)
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

    protected def setTemplateTeams(templateId: Template.ID, teams: Seq[Team])
                                  (implicit session: User.Session): IO[Map[String, String]] =
      client.setTemplateTeams(templateId, teams.toList)
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

    protected def setTemplateScriptUser(templateId: ID, scriptUser: Option[User])
                                       (implicit session: User.Session): IO[Unit] =
      client.setTemplateScriptUser(templateId, scriptUser.getOrElse(session.user))
        .discardU
        .io

    protected def create(templateId: Template.ID, createReleaseArgs: CreateReleaseArgs)
                        (implicit session: User.Session): IO[Release.ID] =
      client.createRelease(templateId, createReleaseArgs)
        .asJson
        .io
        .flatMap {
          case JsObject(r) => r.get("id") match {
            case Some(JsString(id)) => IO.pure(id.replaceFirst("Applications/", ""))
            case _ => IO.raiseError(new RuntimeException("Cannot extract Release ID"))
          }
          case _ => IO.raiseError(new RuntimeException("not a Js object"))
        }

    protected def start(releaseId: Release.ID)
                       (implicit session: User.Session): IO[Release.ID] =
      client.startRelease(releaseId)
        .discard(_ => releaseId)
        .io

    protected def getTasksByTitle(releaseId: Release.ID, taskTitle: String, phaseTitle: Option[String])
                                 (implicit session: User.Session): IO[Set[Task.ID]] =
      client.getTaskByTitle(releaseId, taskTitle, phaseTitle).io.map {
        case JsArray(tasks) => tasks.toList.collect {
          case JsObject(fields) =>
            fields.get("id").flatMap {
              case JsString(fullId) => fullId.split("/").toList match {
                case "Applications" :: `releaseId` :: phaseId :: Nil =>
                  None
                case "Applications" :: `releaseId` :: phaseId :: taskId =>
                  Some(Task.ID(Phase.ID(releaseId, phaseId), taskId.mkString("/")))
                case _ =>
                  None
              }
              case _ => None
            }
          case _ => None
        }.sequence.map(_.toSet).getOrElse(Set.empty)
        case _ => Set.empty
      }

    override protected def waitFor(releaseId: ID, expectedStatus: ReleaseStatus,
                                   interval: Duration = 5 seconds, retries: Option[Int] = Some(20))
                                  (implicit session: User.Session): IO[Unit] = {
      implicit val timeout: Timeout = Timeout(10 seconds)

      getReleaseStatus(releaseId)
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

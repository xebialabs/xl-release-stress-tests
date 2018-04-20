package com.xebialabs.xlrelease.stress.handlers.io.xlr

import akka.http.scaladsl.model.Uri
import akka.stream.Materializer
import akka.util.Timeout
import cats.Show
import cats.effect.IO
import cats.implicits._
import cats.syntax._
import com.github.nscala_time.time.Imports.DateTime
import com.xebialabs.xlrelease.stress.api.xlr.Releases
import com.xebialabs.xlrelease.stress.api.xlr.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.domain.Release.ID
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.handlers.akkaClient.AkkaHttpXlrClient
import com.xebialabs.xlrelease.stress.utils.DateFormat
import com.xebialabs.xlrelease.stress.utils.JsUtils._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class ReleasesHandler()
                     (implicit val
                      server: XlrServer,
                      client: AkkaHttpXlrClient,
                      ec: ExecutionContext,
                      m: Materializer) extends XlrRest {

  implicit def releasesHandler: Releases.Handler[IO] = new Releases.Handler[IO] with DefaultJsonProtocol with DateFormat {
    protected def importTemplate(template: Template)
                                (implicit session: User.Session): IO[Template.ID] =
      client.postZip(
        api(_ / "templates" / "import"),
        template.xlrTemplate
      ).asJson.io >>=
        getFirstId
          .toIO(s"importTemplate(${template.name}): could not read Template Id")

    protected def setTemplateTeams(templateId: Template.ID, teams: Seq[Team])
                                  (implicit session: User.Session): IO[Map[String, String]] =
      client.postJSON(
        api(_ / "templates" / "Applications" / templateId / "teams"),
        teams.map(_.toJson).toJson
      ).asJson.io >>=
        readTeamIds
          .toIO(s"setTemplateTeams($templateId, ${teams.show}): could not read Teams Ids")

    protected def setTemplateScriptUser(templateId: ID, scriptUser: Option[User])
                                       (implicit session: User.Session): IO[Unit] = {
      val user = scriptUser.getOrElse(session.user)
      client.putJSON(
        api(_ / "templates" / "Applications" / templateId),
        JsObject(
          "id" -> JsNull,
          "scheduledStartDate" -> DateTime.now.toJson,
          "type" -> "xlrelease.Release".toJson,
          "scriptUsername" -> user.username.toJson,
          "scriptUserPassword" -> user.password.toJson
        )
      ).discardU.io
    }

    protected def createFromTemplate(templateId: Template.ID, createReleaseArgs: CreateReleaseArgs)
                                    (implicit session: User.Session): IO[Release.ID] =
      client.postJSON(
        api(_ / "templates" / "Applications" / templateId / "create"),
        createReleaseArgs.toJson
      ).asJson.io >>=
        readReleaseId
          .toIO(s"createFromTemplate($templateId, ${createReleaseArgs.show}): Cannot read Release Id")

    protected def createRelease(title: String, scriptUser: Option[User])
                               (implicit session: User.Session): IO[Phase.ID] = {
      val user: User = scriptUser.getOrElse(session.user)
      client.postJSON(
        server(_ / "releases"),
        JsObject(
          "templateId" -> JsNull,
          "title" -> title.toJson,
          "scheduledStartDate" -> DateTime.now.toJson,
          "dueDate" -> (DateTime.now plusHours 5).toJson,
          "type" -> "xlrelease.Release".toJson,
          "owner" -> JsObject(
            "username" -> session.user.username.toJson
          ),
          "scriptUsername" -> JsObject(
            "username" -> user.username.toJson
          ),
          "scriptUserPassword" -> user.password.toJson
        )
      ).asJson.io >>=
        readFirstPhaseId(sep = "-")
          .toIO(s"createRelease($title, $scriptUser): Cannot read Phase Id")
    }

    protected def start(releaseId: Release.ID)
                       (implicit session: User.Session): IO[Release.ID] =
      client.postJSON(
        api(_ / "releases" / "Applications" / releaseId / "start"),
        JsNull
      ).discard(_ => releaseId).io

    protected def getTasksByTitle(releaseId: Release.ID, taskTitle: String, phaseTitle: Option[String])
                                 (implicit session: User.Session): IO[Seq[Task.ID]] = {
      val baseQuery = Uri.Query(
        "releaseId" -> s"Applications/$releaseId",
        "taskTitle" -> taskTitle
      )
      val query = phaseTitle.fold(baseQuery) { pt =>
        ("phaseTitle" -> pt) +: baseQuery
      }
      client.getJSON(
        api(_ / "tasks" / "byTitle").withQuery(query)
      ).io >>=
        readTaskIds(sep = "/")
          .toIO(s"getTasksByTitle($releaseId, $taskTitle, $phaseTitle): Cannot read Task Ids for title '$taskTitle' in Release [$releaseId]")
    }

    override protected def waitFor(releaseId: ID, expectedStatus: ReleaseStatus,
                                   interval: FiniteDuration = 5 seconds, retries: Option[Int] = Some(20))
                                  (implicit session: User.Session): IO[Unit] = {
      implicit val timeout: Timeout = Timeout(10 seconds)

      getReleaseStatus(releaseId)
        .until(_.contains(expectedStatus), interval, retries)
    }
  }

  def getReleaseStatus(releaseId: Release.ID)(implicit session: User.Session): () => IO[JsParsed[ReleaseStatus]] =
    () =>
      client.getJSON(api(_ / "releases" / "Applications" / releaseId))
      .io.map(readReleaseStatus)


  implicit val showTeams: Show[Seq[Team]] = _.map(_.teamName).mkString("[", ", ", "]")

  implicit val showCreateReleaseArgs: Show[CreateReleaseArgs] = {
    case CreateReleaseArgs(title, variables, passwordVariables, scheduledStartDate, autoStart) => {
      val vars: String = (variables ++ passwordVariables).map({ case (k,v) => s"$k:$v"}).mkString("{", " ", "}")
      s"$title $vars @${scheduledStartDate.toString} [auto: $autoStart]"
    }
  }
}

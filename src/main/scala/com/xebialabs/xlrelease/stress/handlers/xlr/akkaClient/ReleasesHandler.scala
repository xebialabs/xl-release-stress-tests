package com.xebialabs.xlrelease.stress.handlers.xlr.akkaClient

import akka.stream.Materializer
import akka.util.Timeout
import cats.Show
import cats.effect.IO
import cats.implicits._
import com.xebialabs.xlrelease.stress.api.xlr.Releases
import com.xebialabs.xlrelease.stress.api.xlr.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.domain.Release.ID
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.utils.JsUtils._
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
        .io >>= getFirstId
          .toIO(s"importTemplate(${template.name}): could not read Template Id")

    protected def setTemplateTeams(templateId: Template.ID, teams: Seq[Team])
                                  (implicit session: User.Session): IO[Map[String, String]] =
      client.setTemplateTeams(templateId, teams.toList)
        .asJson
        .io >>= readTeamIds
          .toIO(s"setTemplateTeams($templateId, ${teams.show}): could not read Teams Ids")

    protected def setTemplateScriptUser(templateId: ID, scriptUser: Option[User])
                                       (implicit session: User.Session): IO[Unit] =
      client.setTemplateScriptUser(templateId, scriptUser.getOrElse(session.user))
        .discardU
        .io

    protected def createFromTemplate(templateId: Template.ID, createReleaseArgs: CreateReleaseArgs)
                                    (implicit session: User.Session): IO[Release.ID] =
      client.createReleaseFromTemplate(templateId, createReleaseArgs)
        .asJson
        .io >>= readReleaseId
          .toIO(s"createFromTemplate($templateId, ${createReleaseArgs.show}): Cannot read Release Id")

    protected def createRelease(title: String, scriptUser: Option[User])
                               (implicit session: User.Session): IO[Phase.ID] =
      client.createRelease(title, scriptUser.getOrElse(session.user))
        .asJson
        .io >>= readFirstPhaseId(sep = "-")
          .toIO(s"createRelease($title, $scriptUser): Cannot read Phase Id")

    protected def start(releaseId: Release.ID)
                       (implicit session: User.Session): IO[Release.ID] =
      client.startRelease(releaseId)
        .discard(_ => releaseId)
        .io

    protected def getTasksByTitle(releaseId: Release.ID, taskTitle: String, phaseTitle: Option[String])
                                 (implicit session: User.Session): IO[Seq[Task.ID]] =
      client.getTaskByTitle(releaseId, taskTitle, phaseTitle)
        .io >>= readTaskIds(sep = "/")
          .toIO(s"getTasksByTitle($releaseId, $taskTitle, $phaseTitle): Cannot read Task Ids for title '$taskTitle' in Release [$releaseId]")

    override protected def waitFor(releaseId: ID, expectedStatus: ReleaseStatus,
                                   interval: Duration = 5 seconds, retries: Option[Int] = Some(20))
                                  (implicit session: User.Session): IO[Unit] = {
      implicit val timeout: Timeout = Timeout(10 seconds)

      getReleaseStatus(releaseId)
        .until(_.contains(expectedStatus), interval, retries)
        .io
    }
  }

  def getReleaseStatus(releaseId: Release.ID)(implicit session: User.Session): () => Future[JsParsed[ReleaseStatus]] =
    () =>
      client.getRelease(releaseId)
        .map(readReleaseStatus)


  implicit val showTeams: Show[Seq[Team]] = _.map(_.teamName).mkString("[", ", ", "]")

  implicit val showCreateReleaseArgs: Show[CreateReleaseArgs] = {
    case CreateReleaseArgs(title, variables, passwordVariables, scheduledStartDate, autoStart) => {
      val vars: String = (variables ++ passwordVariables).map({ case (k,v) => s"$k:$v"}).mkString("{", " ", "}")
      s"$title $vars @${scheduledStartDate.toString} [auto: $autoStart]"
    }
  }
}

package com.xebialabs.xlrelease.stress.dsl.libs.xlr

import akka.http.scaladsl.model.Uri
import cats.implicits._
import com.github.nscala_time.time.Imports.DateTime
import com.xebialabs.xlrelease.stress.config.XlrServer
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.dsl.DSL
import com.xebialabs.xlrelease.stress.dsl.libs.xlr.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.utils.{DateFormat, JsUtils}
import freestyle.free._
import freestyle.free.implicits._
import spray.json._

import scala.concurrent.duration._
import scala.language.postfixOps


class Releases[F[_]](server: XlrServer)(implicit protected val _api: DSL[F]) extends XlrLib[F] with DefaultJsonProtocol with DateFormat {
  def createFromTemplate(templateId: Template.ID, createReleaseArgs: CreateReleaseArgs)
                        (implicit session: User.Session): Program[Release.ID] =
    for {
      _ <- log.debug(s"xlr.releases.createFromTeamplate($templateId, ${createReleaseArgs.show})")
      resp <- lib.http.json.post(server.api(_ ?/ "templates" / "Applications" / templateId.id / "create"), createReleaseArgs.toJson)
      releaseId <- lib.json.parse(JsUtils.readIdString)(resp)
    } yield Release.ID(releaseId)

  def createRelease(title: String, scriptUser: Option[User] = None)
                   (implicit session: User.Session): Program[Phase.ID] = {
    val user: User = scriptUser.getOrElse(session.user)
    for {
      _ <- log.debug(s"xlr.releases.createRelease($title, ${scriptUser.map(_.show)})")
      resp <- lib.http.json.post(server.root(_ ?/ "releases"),
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
        ))
      phaseId <- lib.json.parse(JsUtils.readFirstPhaseId(sep = "-"))(resp)
    } yield phaseId
  }

  def start(releaseId: Release.ID)
           (implicit session: User.Session): Program[Release.ID] =
    for {
      _ <- log.debug(s"xlr.releases.start($releaseId)")
      resp <- lib.http.json.post(server.api(_ ?/ "releases" / "Applications" / releaseId.id / "start"), JsNull)
      _ <- api.http.discard(resp)
    } yield releaseId

  def abort(releaseId: Release.ID)
           (implicit session: User.Session): Program[Release.ID] =
    for {
      _ <- log.debug(s"xlr.releases.abort($releaseId)")
      resp <- lib.http.json.post(server.api(_ ?/ "releases" / "Applications" / releaseId.id / "abort"), JsNull)
      _ <- api.http.discard(resp)
    } yield releaseId

  def getTasksByTitle(releaseId: Release.ID, taskTitle: String, phaseTitle: Option[String] = None)
                     (implicit session: User.Session): Program[Seq[Task.ID]] = {
    val baseQuery = Uri.Query(
      "releaseId" -> s"Applications/$releaseId",
      "taskTitle" -> taskTitle
    )
    val query = phaseTitle.fold(baseQuery) { pt =>
      ("phaseTitle" -> pt) +: baseQuery
    }
    for {
      _ <- log.debug(s"xlr.releases.getTasksByTitle($releaseId, $taskTitle, $phaseTitle)")
      content <- lib.http.json.get(server.api(_ ?/ "tasks" / "byTitle").withQuery(query))
      taskIds <- lib.json.read(JsUtils.readTaskIds(sep = "/"))(content)
    } yield taskIds
  }

  def search(planned: Boolean, active: Boolean)
            (implicit session: User.Session): Program[JsArray] = {
    val query = Uri.Query(
      "page" -> "0",
      "resultsPerPage" -> "100"
    )
    for {
      _ <- log.debug(s"xlr.releases.search(planned = $planned, active = $active)")
      resp <- lib.http.json.post(server.api(_ ?/ "releases" / "search").withQuery(query), JsObject(
        "planned" -> planned.toJson,
        "active" -> active.toJson,
        "inactive" -> false.toJson
      ))
      array <- lib.json.parse(JsUtils.jsArray)(resp)
    } yield array
  }

  def waitFor(releaseId: Release.ID, status: ReleaseStatus, interval: FiniteDuration = 5 seconds, retries: Option[Int] = None)
             (implicit session: User.Session): Program[Unit] =
    for {
      _ <- log.debug(s"xlr.releases.waitFor($releaseId, $status, $interval, $retries)")
      _ <- lib.control.until[ReleaseStatus](_ == status, interval, retries) {
        getReleaseStatus(releaseId)
      }
    } yield ()

  def getReleaseStatus(releaseId: Release.ID)
                      (implicit session: User.Session): Program[ReleaseStatus] =
    for {
      _ <- log.debug(s"xlr.releases.getReleaseStatus($releaseId)")
      content <- lib.http.json.get(server.api(_ ?/ "releases" / "Applications" / releaseId.id))
      releaseStatus <- lib.json.read(JsUtils.readReleaseStatus)(content)
    } yield releaseStatus


}


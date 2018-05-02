package com.xebialabs.xlrelease.stress.dsl.libs.xlr


import akka.http.scaladsl.model.Uri
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.XlrServer
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.dsl.DSL
import com.xebialabs.xlrelease.stress.utils.JsUtils
import freestyle.free._
import freestyle.free.implicits._
import spray.json._


class Phases[F[_]](server: XlrServer)(implicit protected val _api: DSL[F]) extends XlrLib[F] with DefaultJsonProtocol {

  def appendTask(phaseId: Phase.ID, title: String, taskType: String)
                (implicit session: User.Session): Program[Task.ID] =
    for {
      _ <- log.debug(s"xlr.tasks.appendToPhase(${phaseId.show}, $title, $taskType)")
      resp <- lib.http.json.post(server.api(_ / "tasks" / "Applications" / phaseId.release / phaseId.phase / "tasks"),
        JsObject(
          "id" -> JsNull,
          "title" -> title.toJson,
          "type" -> taskType.toJson
        ))
      taskId <- lib.json.parse(JsUtils.readTaskId(sep = "/"))(resp)
    } yield taskId


  def insertTask(phaseId: Phase.ID, title: String, taskType: String, position: Int = 0)
                (implicit session: User.Session): Program[Task.ID] = {
    require(position >= 0)
    for {
      _ <- log.debug(s"xlr.tasks.insertInPhase(${phaseId.show}, $title, $taskType, $position)")
      resp <- lib.http.json.post(
        server.api(_ / "phases" / "Applications" / phaseId.release / phaseId.phase / "tasks").withQuery(
          Uri.Query("position" -> position.toString)
        ),
        JsObject(
          "id" -> JsNull,
          "title" -> title.toJson,
          "type" -> taskType.toJson
        )
      )
      taskId <- lib.json.parse(JsUtils.readTaskId(sep = "/"))(resp)
    } yield taskId
  }
}
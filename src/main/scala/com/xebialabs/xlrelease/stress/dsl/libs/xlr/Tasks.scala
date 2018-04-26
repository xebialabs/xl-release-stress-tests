package com.xebialabs.xlrelease.stress.dsl.libs.xlr

import cats.implicits._
import com.xebialabs.xlrelease.stress.config.{XlrConfig, XlrServer}
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.dsl
import com.xebialabs.xlrelease.stress.dsl.DSL
import com.xebialabs.xlrelease.stress.dsl.http.libs
import com.xebialabs.xlrelease.stress.utils.DateFormat
import com.xebialabs.xlrelease.stress.utils.JsUtils._
import freestyle.free._
import freestyle.free.implicits._
import spray.json._

import scala.concurrent.duration._
import scala.language.postfixOps


class Tasks[F[_]](server: XlrServer)(implicit protected val _api: DSL[F]) extends XlrLib[F] with DefaultJsonProtocol {

  def appendScriptTask(phaseId: Phase.ID, title: String, taskType: String, script: String)
                      (implicit session: User.Session): Program[Task.ID] =
    for {
      _ <- log.debug(s"xlr.tasks.appendScriptTask(${phaseId.show}, $title, $taskType, $script)")
      resp <- lib.http.json.post(server.api(_ / "tasks" / "Applications" / phaseId.release / phaseId.phase / "tasks"),
        JsObject(
          "id" -> JsNull,
          "title" -> title.toJson,
          "type" -> taskType.toJson,
          "script" -> script.toJson
        ))
      content <- api.http.parseJson(resp)
      taskId <- lib.json.read(readTaskId(sep = "/"))(content)
    } yield taskId

  def appendManualTask(phaseId: Phase.ID, title: String)
                      (implicit session: User.Session): Program[Task.ID] =
    for {
      _ <- log.debug(s"xlr.tasks.appendManualTask(${phaseId.show}, $title)")
      resp <- lib.http.json.post(server.api(_ / "tasks" / "Applications" / phaseId.release / phaseId.phase / "tasks"),
        JsObject(
          "id" -> JsNull,
          "title" -> title.toJson,
          "type" -> "xlrelease.Task".toJson
        ))
      content <- api.http.parseJson(resp)
      taskId <- lib.json.read(readTaskId(sep = "/"))(content)
    } yield taskId

  def assignTo(taskId: Task.ID, assignee: User.ID)
              (implicit session: User.Session): Program[Unit] =
    for {
      _ <- log.debug(s"xlr.tasks.assignTo(${taskId.show}, $assignee")
      resp <- lib.http.json.post(
        server.api(_ / "tasks" / "Applications" ++ taskId.path / "assign" / assignee),
        JsNull
      )
      _ <- api.http.discard(resp)
    } yield ()

  def complete(taskId: Task.ID, comment: Option[String] = None)
              (implicit session: User.Session): Program[Boolean] =
    for {
      _ <- log.debug(s"xlr.tasks.complete(${taskId.show}, $comment)")
      resp <- lib.http.json.post(
        server.api(_ / "tasks" / "Applications" ++ taskId.path / "complete"),
        comment.map(content => JsObject("comment" -> content.toJson)).getOrElse(JsObject.empty)
      )
      content <- api.http.parseJson(resp)
    } yield matchesTaskStatus(TaskStatus.Completed)(content)

  def retry(taskId: Task.ID, comment: String)
           (implicit session: User.Session): Program[Comment.ID] = notImplemented("retry")

  def skip(taskId: Task.ID, comment: String)
          (implicit session: User.Session): Program[Comment.ID] = notImplemented("skip")


  def waitFor(taskId: Task.ID,
              expectedStatus: TaskStatus = TaskStatus.InProgress,
              interval: FiniteDuration = 5 seconds,
              retries: Option[Int] = Some(20))
             (implicit session: User.Session): Program[Unit] = {
    for {
      _ <- log.debug(s"xlr.tasks.waitFor(${taskId.show}, $expectedStatus, $interval, $retries")
      _ <- lib.control.until[TaskStatus](_ == expectedStatus, interval, retries) {
        getTaskStatus(taskId)
      }
    } yield ()

  }

  def getTaskStatus(taskId: Task.ID)
                   (implicit session: User.Session): Program[TaskStatus] =
    for {
      _ <- log.debug(s"xlr.tasks.getTaskStatus(${taskId.show})")
      resp <- lib.http.json.post(server.root(_ / "tasks" / "poll"), JsObject("ids" -> Seq(taskId.show).toJson))
      content <- api.http.parseJson(resp)
      taskStatus <- lib.json.read(readFirstTaskStatus)(content)
    } yield taskStatus


  def getComments(taskId: Task.ID)
                 (implicit session: User.Session): Program[Seq[Comment]] =
    for {
      _ <- log.debug(s"xlr.tasks.getComments(${taskId.show})")
      content <- lib.http.json.get(server.api(_ / "tasks" / "Applications" ++ taskId.path))
      comments <- lib.json.read(readComments)(content)
    } yield comments

  private def notImplemented[A](name: String): Program[A] =
    api.error.error[A](new RuntimeException(s"Not implemented: $name"))
}

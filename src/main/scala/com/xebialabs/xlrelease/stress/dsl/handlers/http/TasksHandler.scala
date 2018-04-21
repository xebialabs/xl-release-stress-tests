package com.xebialabs.xlrelease.stress.dsl.handlers.http

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.{Cookie, `Set-Cookie`}
import cats.implicits._
import com.github.nscala_time.time.Imports.DateTime
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.dsl.handlers.io.xlr.XlrRest
import com.xebialabs.xlrelease.stress.dsl.xlr
import com.xebialabs.xlrelease.stress.dsl.xlr.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.http
import com.xebialabs.xlrelease.stress.http.{ClientLib, HttpLib}
import com.xebialabs.xlrelease.stress.utils.DateFormat
import com.xebialabs.xlrelease.stress.utils.JsUtils._
import freestyle.free._
import freestyle.free.implicits._
import spray.json._

import scala.concurrent.duration._
import scala.language.postfixOps


class TasksHandler[F[_]]()
                        (implicit val
                         server: XlrServer,
                         adminPassword: AdminPassword,
                         client: http.Client[F],
                         target: http.Http[F]) extends XlrRest {

  val httpLib = new HttpLib[F]()

  type Target[A] = FreeS[F, A]

  implicit def xlrTasksHandler: xlr.Tasks.Handler[Target] = new xlr.Tasks.Handler[Target] with DefaultJsonProtocol with DateFormat {
    protected def appendScriptTask(phaseId: Phase.ID, title: String, taskType: String, script: String)
                                 (implicit session: User.Session): Target[Task.ID] =
      for {
        _ <- debug(s"appendScriptTask(${phaseId.show}, $title, $taskType, $script)")
        resp <- httpLib.client.postJSON(api(_ / "tasks" / "Applications" / phaseId.release / phaseId.phase / "tasks"),
          JsObject(
            "id" -> JsNull,
            "title" -> title.toJson,
            "type" -> taskType.toJson,
            "script" -> script.toJson
          ))
        content <- target.client.parseJson(resp)
        taskId <- target.json.read(readTaskId(sep = "/"))(content)
      } yield taskId

    protected def assignTo(taskId: Task.ID, assignee: User.ID)(implicit session: User.Session): Target[Unit] =
      for {
        _ <- debug(s"assignTo(${taskId.show}, $assignee")
        resp <- httpLib.client.postJSON(
          api(_ / "tasks" / "Applications" / taskId.release / taskId.phase / taskId.task / "assign" / assignee),
          JsNull
        )
        _ <- target.client.discard(resp)
      } yield ()

    protected def complete(taskId: Task.ID, comment: Option[String] = None)
                          (implicit session: User.Session): Target[Boolean] =
      for {
        _ <- debug(s"complete(${taskId.show}, $comment)")
        resp <- httpLib.client.postJSON(
          api(_ / "tasks" / "Applications" / taskId.release / taskId.phase / taskId.task / "complete"),
          comment.map(content => JsObject("comment" -> content.toJson)).getOrElse(JsObject.empty)
        )
        content <- target.client.parseJson(resp)
      } yield matchesTaskStatus(TaskStatus.Completed)(content)

    protected def retry(taskId: Task.ID, comment: String)
                       (implicit session: User.Session): Target[Comment.ID] = notImplemented("retry")

    protected def skip(taskId: Task.ID, comment: String)
                      (implicit session: User.Session): Target[Comment.ID] = notImplemented("skip")


    protected def waitFor(taskId: Task.ID, expectedStatus: TaskStatus = TaskStatus.InProgress,
                          interval: FiniteDuration = 5 seconds, retries: Option[Int] = Some(20))
                         (implicit session: User.Session): Target[Unit] = {
      def getTaskStatus: Target[TaskStatus] =
        for {
          _ <- debug(s"waitFor: getTaskStatus(${taskId.show}, $expectedStatus, $interval, $retries)")
          resp <- httpLib.client.postJSON(root(_ / "tasks" / "poll"), JsObject("ids" -> Seq(taskId.show).toJson))
          content <- target.client.parseJson(resp)
          taskStatus <- target.json.read(readFirstTaskStatus)(content)
        } yield taskStatus

      httpLib.until[TaskStatus](_ == expectedStatus, interval, retries)(getTaskStatus)
        .map(_ => ())

    }
  }

  private def debug(msg: String)(implicit session: User.Session): Target[Unit] =
    target.log.debug(s"${session.user.username}: $msg")

  private def notImplemented[A](name: String): Target[A] =
    target.error.error[A](new RuntimeException(s"Not implemented: $name"))

}

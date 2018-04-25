package com.xebialabs.xlrelease.stress.handlers.xlr.http

import cats.implicits._
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.handlers.xlr.XlrRest
import com.xebialabs.xlrelease.stress.dsl.xlr
import com.xebialabs.xlrelease.stress.dsl.http.{Client, Http, HttpLib}
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
                         client: Client[F],
                         http: Http[F]) extends XlrRest {

  val httpLib = new HttpLib[F]()

  type Target[A] = FreeS[F, A]

  implicit def tasksHandler: xlr.Tasks.Handler[Target] = new xlr.Tasks.Handler[Target] with DefaultJsonProtocol with DateFormat {
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
        content <- http.client.parseJson(resp)
        taskId <- http.json.read(readTaskId(sep = "/"))(content)
      } yield taskId

    protected def appendManualTask(phaseId: Phase.ID, title: String)
                                  (implicit session: User.Session): Target[Task.ID] =
      for {
        _ <- debug(s"appendManualTask(${phaseId.show}, $title)")
        resp <- httpLib.client.postJSON(api(_ / "tasks" / "Applications" / phaseId.release / phaseId.phase / "tasks"),
          JsObject(
            "id" -> JsNull,
            "title" -> title.toJson,
            "type" -> "xlrelease.Task".toJson
          ))
        content <- http.client.parseJson(resp)
        taskId <- http.json.read(readTaskId(sep = "/"))(content)
      } yield taskId

    protected def assignTo(taskId: Task.ID, assignee: User.ID)(implicit session: User.Session): Target[Unit] =
      for {
        _ <- debug(s"assignTo(${taskId.show}, $assignee")
        resp <- httpLib.client.postJSON(
          api(_ / "tasks" / "Applications" / taskId.release / taskId.phase / taskId.task / "assign" / assignee),
          JsNull
        )
        _ <- http.client.discard(resp)
      } yield ()

    protected def complete(taskId: Task.ID, comment: Option[String] = None)
                          (implicit session: User.Session): Target[Boolean] =
      for {
        _ <- debug(s"complete(${taskId.show}, $comment)")
        resp <- httpLib.client.postJSON(
          api(_ / "tasks" / "Applications" / taskId.release / taskId.phase / taskId.task / "complete"),
          comment.map(content => JsObject("comment" -> content.toJson)).getOrElse(JsObject.empty)
        )
        content <- http.client.parseJson(resp)
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
          content <- http.client.parseJson(resp)
          taskStatus <- http.json.read(readFirstTaskStatus)(content)
        } yield taskStatus

      httpLib.until[TaskStatus](_ == expectedStatus, interval, retries)(getTaskStatus)
        .map(_ => ())

    }

    protected def getComments(taskId: Task.ID)
                             (implicit session: User.Session): Target[Seq[Comment]] =
      for {
        _ <- debug(s"getComments(${taskId.show})")
        resp <- http.client.get(api(_ / "tasks" / "Applications" / taskId.release / taskId.phase / taskId.task))
        content <- http.client.parseJson(resp)
        comments <- http.json.read(readComments)(content)
      } yield comments
  }

  private def debug(msg: String)(implicit session: User.Session): Target[Unit] =
    http.log.debug(s"${session.user.username}: $msg")

  private def notImplemented[A](name: String): Target[A] =
    http.error.error[A](new RuntimeException(s"Not implemented: $name"))

}

package com.xebialabs.xlrelease.stress.handlers.xlr.io

import akka.stream.Materializer
import akka.util.Timeout
import cats.Show
import cats.effect.IO
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.XlrServer
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.dsl.xlr.Tasks
import com.xebialabs.xlrelease.stress.handlers.xlr.XlrRest
import com.xebialabs.xlrelease.stress.utils.AkkaHttpClient
import com.xebialabs.xlrelease.stress.utils.JsUtils._
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class TasksHandler()
                  (implicit val
                   server: XlrServer,
                   client: AkkaHttpClient,
                   ec: ExecutionContext,
                   m: Materializer,
                   s: Show[TaskStatus]) extends XlrRest with DefaultJsonProtocol {

  implicit def tasksHandler: Tasks.Handler[IO] = new Tasks.Handler[IO] with DefaultJsonProtocol {

    protected def appendScriptTask(phaseId: Phase.ID, title: String, taskType: String, script: String)
                                  (implicit session: User.Session): IO[Task.ID] =
      client.postJSON(
        api(_ / "tasks" / "Applications" / phaseId.release / phaseId.phase / "tasks"),
        JsObject(
          "id" -> JsNull,
          "title" -> title.toJson,
          "type" -> taskType.toJson,
          "script" -> script.toJson
        )
      ).asJson.io >>=
        readTaskId(sep = "/")
          .toIO(s"appendScriptTask(${phaseId.show}, $title, $taskType): failed to read Task Id")

    protected def appendManualTask(phaseId: Phase.ID, title: String)
                                  (implicit session: User.Session): IO[Task.ID] =
      client.postJSON(
        api(_ / "tasks" / "Applications" / phaseId.release / phaseId.phase / "tasks"),
        JsObject(
          "id" -> JsNull,
          "title" -> title.toJson,
          "type" -> "xlrelease.Task".toJson
        )
      ).asJson.io >>=
        readTaskId(sep = "/")
          .toIO(s"appendManualTask(${phaseId.show}, $title): failed to read Task Id")

    protected def assignTo(taskId: Task.ID, assignee: User.ID)
                          (implicit session: User.Session): IO[Unit] =
      client.postJSON(
        api(_ / "tasks" / "Applications" / taskId.release / taskId.phase / taskId.task / "assign" / assignee),
        JsNull
      ).discardU.io

    protected def complete(taskId: Task.ID, comment: Option[String] = None)
                          (implicit session: User.Session): IO[Boolean] =
      client.postJSON(
        api(_ / "tasks" / "Applications" / taskId.release / taskId.phase / taskId.task / "complete"),
        comment.map(content => JsObject("comment" -> content.toJson)).getOrElse(JsObject.empty)
      ).asJson
        .map(matchesTaskStatus(TaskStatus.Completed))
        .io

    protected def retry(taskId: Task.ID, comment: String)
                       (implicit session: User.Session): IO[Comment.ID] = notImplemented("retry")

    protected def skip(taskId: Task.ID, comment: String)
                      (implicit session: User.Session): IO[Comment.ID] = notImplemented("skip")


    protected def waitFor(taskId: Task.ID, expectedStatus: TaskStatus = TaskStatus.InProgress,
                          interval: FiniteDuration = 5 seconds, retries: Option[Int] = Some(20))
                         (implicit session: User.Session): IO[Unit] = {
      implicit val timeout: Timeout = Timeout(10 seconds)

      getTaskStatus(taskId)
        .until(_ == expectedStatus, interval, retries)
    }

    protected def getComments(taskId: Task.ID)
                             (implicit session: User.Session): IO[Seq[Comment]] =
      client.getJSON(api(_ / "tasks" / "Applications" / taskId.release / taskId.phase / taskId.task))
        .io >>= readComments
          .toIO("")
  }

  def getTaskStatus(taskId: Task.ID)(implicit session: User.Session): () => IO[TaskStatus] =
    () =>
      client.postJSON(
        root(_ / "tasks" / "poll"),
        JsObject("ids" -> Seq(taskId.show).toJson)
      ).asJson.io map readFirstTaskStatus flatMap {
        case Left(err) => IO.raiseError(err)
        case Right(value) => IO.pure(value)
      }

  def notImplemented[A](name: String): IO[A] =
    IO.raiseError(new RuntimeException("not implemented: "+ name))

}

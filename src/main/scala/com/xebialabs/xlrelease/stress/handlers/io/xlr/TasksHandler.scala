package com.xebialabs.xlrelease.stress.handlers.io.xlr

import akka.stream.Materializer
import akka.util.Timeout
import cats.Show
import cats.implicits._
import cats.effect.IO
import com.xebialabs.xlrelease.stress.api.xlr.Tasks
import com.xebialabs.xlrelease.stress.config.XlrServer
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.http.AkkaHttpClient
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

  def notImplemented[A](name: String): IO[A] = IO.raiseError(new RuntimeException("not implemented: "+ name))

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
        .until(_.contains(expectedStatus), interval, retries)
    }
  }

  def getTaskStatus(taskId: Task.ID)(implicit session: User.Session): () => IO[JsParsed[TaskStatus]] =
    () =>
      client.postJSON(
        root(_ / "tasks" / "poll"),
        JsObject("ids" -> Seq(taskId.show).toJson)
      ).asJson.io
        .map(readFirstTaskStatus)

}

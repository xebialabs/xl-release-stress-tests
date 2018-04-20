package com.xebialabs.xlrelease.stress.handlers.xlr.akkaClient

import akka.stream.Materializer
import akka.util.Timeout
import cats.Show
import cats.implicits._
import cats.effect.IO
import com.xebialabs.xlrelease.stress.api.xlr.Tasks
import com.xebialabs.xlrelease.stress.domain.{Comment, Task, TaskStatus, User}
import com.xebialabs.xlrelease.stress.domain.User.Session
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class TasksHandler(implicit val client: AkkaHttpXlrClient, ec: ExecutionContext, m: Materializer, s: Show[TaskStatus]) {

  def notImplemented[A](name: String): IO[A] = IO.raiseError(new RuntimeException("not implemented: "+ name))

  implicit def tasksHandler: Tasks.Handler[IO] = new Tasks.Handler[IO] with DefaultJsonProtocol {

    protected def assignTo(taskId: Task.ID, assignee: User.ID)
                          (implicit session: User.Session): IO[Unit] =
      client.assignTaskTo(taskId, assignee)
        .discardU
        .io

    protected def complete(taskId: Task.ID, comment: Option[String] = None)
                          (implicit session: User.Session): IO[Boolean] =
      client.completeTask(taskId, comment).asJson
        .map(matchesTaskStatus(TaskStatus.Completed))
        .io

    protected def retry(taskId: Task.ID, comment: String)
                       (implicit session: User.Session): IO[Comment.ID] = notImplemented("retry")

    protected def skip(taskId: Task.ID, comment: String)
                      (implicit session: User.Session): IO[Comment.ID] = notImplemented("skip")


    protected def waitFor(taskId: Task.ID, expectedStatus: TaskStatus = TaskStatus.InProgress,
                          interval: Duration = 5 seconds, retries: Option[Int] = Some(20))
                         (implicit session: User.Session): IO[Unit] = {
      implicit val timeout: Timeout = Timeout(10 seconds)

      getTaskStatus(taskId)
        .until(_.contains(expectedStatus), interval, retries)
        .io
    }
  }

  def getTaskStatus(taskId: Task.ID)(implicit session: User.Session): () => Future[Option[TaskStatus]] =
    () => client.pollTask(taskId.show)(session).asJson.map(readFirstTaskStatus)


  def readTaskStatus: JsValue => Option[TaskStatus] = {
    case JsObject(fields) =>
      fields.get("status").map(_.convertTo[TaskStatus])
    case _ =>
      None
  }

  def readFirstTaskStatus: JsValue => Option[TaskStatus] = {
    case JsArray(arr) =>
      arr.headOption
        .flatMap(readTaskStatus)
    case _ => None
  }

  def matchesTaskStatus(expectedStatus: TaskStatus): JsValue => Boolean =
    readFirstTaskStatus andThen (_.contains(expectedStatus))
}
package com.xebialabs.xlrelease.stress.client.akkaClient

import akka.stream.Materializer
import akka.util.Timeout
import cats.Show
import cats.implicits._
import com.xebialabs.xlrelease.stress.client.Tasks
import com.xebialabs.xlrelease.stress.domain.{Comment, Task, TaskStatus, User}
import com.xebialabs.xlrelease.stress.domain.User.Session
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class TasksHandler(val client: AkkaHttpXlrClient)(implicit val ec: ExecutionContext, m: Materializer, s: Show[TaskStatus]) {

  def notImplemented[A](name: String): Future[A] = Future.failed(new RuntimeException("not implemented: "+ name))

  implicit def tasksHandler: Tasks.Handler[Future] = new Tasks.Handler[Future] with DefaultJsonProtocol {

    protected def assignTo(session: User.Session, taskId: Task.ID, assignee: User.ID): Future[Unit] =
      client.assignTaskTo(taskId, assignee)(session)
        .discardU

    protected def complete(session: User.Session, taskId: Task.ID, comment: Option[String] = None): Future[Boolean] =
      client.completeTask(taskId, comment)(session).asJson
        .map(matchesTaskStatus(TaskStatus.Completed))

    protected def retry(session: User.Session, taskId: Task.ID, comment: String): Future[Comment.ID] = notImplemented("retry")

    protected def skip(session: User.Session, taskId: Task.ID, comment: String): Future[Comment.ID] = notImplemented("skip")


    protected def waitFor(session: Session, taskId: Task.ID, expectedStatus: TaskStatus = TaskStatus.InProgress,
                          interval: Duration = 5 seconds, retries: Option[Int] = Some(20)): Future[Unit] = {
      implicit val timeout: Timeout = Timeout(10 seconds)

      getTaskStatus(taskId)(session)
        .until(_.contains(expectedStatus), interval, retries)
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
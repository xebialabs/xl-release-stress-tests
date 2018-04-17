package com.xebialabs.xlrelease.stress.client.akkaClient

import akka.stream.Materializer
import cats.Show
import cats.implicits._
import com.xebialabs.xlrelease.stress.client.Tasks
import com.xebialabs.xlrelease.stress.parsers.dataset.User.Session
import com.xebialabs.xlrelease.stress.parsers.dataset._
import spray.json._

import scala.annotation.tailrec
import scala.concurrent.{Await, ExecutionContext, Future}
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

    @tailrec
    protected def waitFor(session: Session, taskId: Task.ID, expectedStatus: TaskStatus = TaskStatus.InProgress): Future[Unit] = {
      val found = Await.result(
        client.pollTask(taskId.show)(session).asJson
          .map(matchesTaskStatus(expectedStatus)),
        10 seconds
      )
      if (found) {
        Future.successful(())
      } else {
        Thread.sleep(5 * 1000)
        waitFor(session, taskId, expectedStatus)
      }
    }

  }

  def getTaskStatus: JsValue => Option[TaskStatus] = {
    case obj: JsObject =>
      obj.getFields("status").headOption.map(_.convertTo[TaskStatus])
    case _ => None
  }

  def matchesTaskStatus(expectedStatus: TaskStatus): JsValue => Boolean = {
    case JsArray(arr) =>
      arr.headOption
        .flatMap(getTaskStatus)
        .contains(expectedStatus)
    case _ => false
  }
}
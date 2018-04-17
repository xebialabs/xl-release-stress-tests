package com.xebialabs.xlrelease.stress.client.akkaClient

import com.xebialabs.xlrelease.stress.client.{Releases, Tasks}
import com.xebialabs.xlrelease.stress.parsers.dataset.Release.ID
import com.xebialabs.xlrelease.stress.parsers.dataset.Team.{releaseAdmin, templateOwner}
import com.xebialabs.xlrelease.stress.parsers.dataset.User.Session
import com.xebialabs.xlrelease.stress.parsers.dataset._

import scala.concurrent.{ExecutionContext, Future}

class TasksHandler(val client: AkkaHttpXlrClient)(implicit val ec: ExecutionContext) {

  def notImplemented[A]: Future[A] = Future.failed(new RuntimeException("not implemented"))

  implicit def tasksHandler: Tasks.Handler[Future] = new Tasks.Handler[Future] {
    protected def complete(session: User.Session, taskId: Task.ID): Future[Unit] = notImplemented

    protected def abort(session: User.Session, taskId: Task.ID, comment: String): Future[Comment.ID] = notImplemented

    protected def retry(session: User.Session, taskId: Task.ID, comment: String): Future[Comment.ID] = notImplemented

    protected def skip(session: User.Session, taskId: Task.ID, comment: String): Future[Comment.ID] = notImplemented

    protected def waitInProgress(session: Session, taskId: Task.ID): Future[Task] = notImplemented
  }
}
package com.xebialabs.xlrelease.stress.api.xlr

import com.xebialabs.xlrelease.stress.domain.{Comment, Task, TaskStatus, User}
import freestyle.free._

import scala.concurrent.duration._
import scala.language.postfixOps

@free trait Tasks {
  def assignTo(taskId: Task.ID, assignee: User.ID)(implicit session: User.Session): FS[Unit]
  def complete(taskId: Task.ID, comment: Option[String] = None)(implicit session: User.Session): FS[Boolean]
  def retry(taskId: Task.ID, comment: String)(implicit session: User.Session): FS[Comment.ID]
  def skip(taskId: Task.ID, comment: String)(implicit session: User.Session): FS[Comment.ID]
  def waitFor(taskId: Task.ID, status: TaskStatus = TaskStatus.InProgress,
              interval: Duration = 5 seconds, retries: Option[Int] = Some(20))
             (implicit session: User.Session): FS[Unit]
}

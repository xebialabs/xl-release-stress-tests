package com.xebialabs.xlrelease.stress.client

import com.xebialabs.xlrelease.stress.parsers.dataset.{Comment, Task, TaskStatus, User}
import freestyle.free._

@free trait Tasks {
  def assignTo(session: User.Session, taskId: Task.ID, assignee: User.ID): FS[Unit]
  def complete(session: User.Session, taskId: Task.ID, comment: Option[String] = None): FS[Boolean]
  def retry(session: User.Session, taskId: Task.ID, comment: String): FS[Comment.ID]
  def skip(session: User.Session, taskId: Task.ID, comment: String): FS[Comment.ID]
  def waitFor(session: User.Session, taskId: Task.ID, status: TaskStatus = TaskStatus.InProgress): FS[Unit]
}

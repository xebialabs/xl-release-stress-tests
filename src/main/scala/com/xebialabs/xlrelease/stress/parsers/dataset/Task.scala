package com.xebialabs.xlrelease.stress.parsers.dataset

//case class Task(id: Task.ID, taskType: String, title: String, subTasks: Seq[Task], comments: Seq[Comment])
case class Task(id: Task.ID, title: String, taskType: String, status: String)

object Task {
  case class ID(phaseId: Phase.ID, taskId: String)

  implicit class TaskIDOps(val taskId: Task.ID) extends AnyVal {
    def releaseId: Release.ID = taskId.phaseId.releaseId
    def show: String = s"Applications/$releaseId/${taskId.phaseId.phaseId}/${taskId.taskId}"
  }
}
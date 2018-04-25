package com.xebialabs.xlrelease.stress.domain

import akka.http.scaladsl.model.Uri
import cats.Show

case class Task(id: Task.ID, title: String, taskType: String, status: String)

object Task {
  case class ID(phaseId: Phase.ID, task: String)

  implicit def showTaskId(implicit sp: Show[Phase.ID]): Show[Task.ID] = {
    case ID(phaseId, taskId) => s"${sp.show(phaseId)}/$taskId"
  }

  implicit class TaskIDOps(val taskId: Task.ID) extends AnyVal {
    def release: Release.ID = taskId.phaseId.release
    def phase: String = taskId.phaseId.phase

    def asList: List[String] = release :: phase :: taskId.task.split("/").toList

    def path: Uri.Path = asList.foldLeft[Uri.Path](Uri./.path) {
      case (l, r) => l / r
    }
  }

}
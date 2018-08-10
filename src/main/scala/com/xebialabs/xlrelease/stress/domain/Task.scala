package com.xebialabs.xlrelease.stress.domain

import akka.http.scaladsl.model.Uri
import cats.Show
import cats.data.NonEmptyList

case class Task(id: Task.ID, title: String, taskType: String, status: String)

object Task {
  case class ID(phaseId: Phase.ID, task: String)

  implicit def showTaskId(implicit sp: Show[Phase.ID]): Show[Task.ID] = {
    case ID(phaseId, taskId) => s"${sp.show(phaseId)}/$taskId"
  }

  implicit class TaskIDOps(val taskId: Task.ID) extends AnyVal {
    def release: Release.ID = taskId.phaseId.release
    def phase: String = taskId.phaseId.phase

    def asNel: NonEmptyList[String] = NonEmptyList(release.id, phase :: taskId.task.split("/").toList)

    def path: Uri.Path = {
      val nel = asNel
      nel.tail.foldLeft[Uri.Path](Uri.Path / nel.head) {
        case (l, r) => l / r
      }
    }
  }

}
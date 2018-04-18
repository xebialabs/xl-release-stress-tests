package com.xebialabs.xlrelease.stress.domain

import cats.Show

case class Task(id: Task.ID, title: String, taskType: String, status: String)

object Task {
  case class ID(phaseId: Phase.ID, taskId: String)

  implicit def showTaskId(implicit sp: Show[Phase.ID]): Show[Task.ID] = {
    case ID(phaseId, taskId) => s"${sp.show(phaseId)}/$taskId"
  }

  implicit class TaskIDOps(val taskId: Task.ID) extends AnyVal {
    def releaseId: Release.ID = taskId.phaseId.releaseId
  }
}
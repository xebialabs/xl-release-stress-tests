package com.xebialabs.xlrelease.stress.domain

import akka.http.scaladsl.model.Uri
import cats.Show
import cats.data.NonEmptyList
import cats.implicits._


sealed trait Target
object Target {
  case class VariableTarget(variableId: Variable.ID) extends Target

  sealed trait ConcreteTarget extends Target
  case class ReleaseTarget(releaseId: Release.ID) extends ConcreteTarget
  case class PhaseTarget(phaseId: Phase.ID) extends ConcreteTarget
  case class TaskTarget(taskId: Task.ID) extends ConcreteTarget

  implicit class ConcreteTargetOps(val target: ConcreteTarget) extends AnyVal {
    def asNel: NonEmptyList[String] = target match {
      case ReleaseTarget(releaseId) => NonEmptyList(releaseId, Nil)
      case PhaseTarget(phaseId) => NonEmptyList(phaseId.release, phaseId.phase :: Nil)
      case TaskTarget(taskId) => NonEmptyList(taskId.release, taskId.phase :: taskId.task.split("/").toList)
    }

    def path: Uri.Path = {
      val nel = asNel
      nel.tail.foldLeft[Uri.Path](Uri.Path / nel.head) {
        case (l, r) => l / r
      }
    }
  }

  implicit class ReleaseTargetOps(val releaseId: Release.ID) extends AnyVal {
    def target: ReleaseTarget = ReleaseTarget(releaseId)
  }

  implicit class PhaseTargetOps(val phaseId: Phase.ID) extends AnyVal {
    def target: PhaseTarget = PhaseTarget(phaseId)
  }

  implicit class TaskTargetOps(val taskId: Task.ID) extends AnyVal {
    def target: TaskTarget = TaskTarget(taskId)
  }

  implicit val showConcreteTarget: Show[ConcreteTarget] = ct => ("Applications" :: ct.asNel.toList).mkString("/")

  implicit val showVariableTarget: Show[VariableTarget] = vt => vt.variableId.show

  implicit val showTarget: Show[Target] = {
    case vt: VariableTarget => showVariableTarget.show(vt)
    case ct: ConcreteTarget => showConcreteTarget.show(ct)
  }
}
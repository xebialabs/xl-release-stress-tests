package com.xebialabs.xlrelease.stress.domain

import akka.http.scaladsl.model.Uri
import cats.Show
import cats.data.NonEmptyList
import cats.implicits._


sealed trait Target
object Target {
  case class VariableTarget(variableId: Variable.ID) extends Target
  case class ConcreteTarget(releaseId: Release.ID, phaseId: Option[String], taskId: Option[String]) extends Target

  implicit class ConcreteTargetOps(val target: ConcreteTarget) extends AnyVal {
    def asNel: NonEmptyList[String] = NonEmptyList(target.releaseId, (target.phaseId :: target.taskId :: Nil).flatten)

    def path: Uri.Path = {
      val nel = asNel
      nel.tail.foldLeft[Uri.Path](Uri.Path / nel.head) {
        case (l, r) => l / r
      }
    }
  }

  implicit val showConcreteTarget: Show[ConcreteTarget] = ct => ("Applications" :: ct.asNel.toList).mkString("/")

  implicit val showVariableTarget: Show[VariableTarget] = vt => vt.variableId.show

  implicit val showTarget: Show[Target] = {
    case vt: VariableTarget => showVariableTarget.show(vt)
    case ct: ConcreteTarget => showConcreteTarget.show(ct)
  }
}
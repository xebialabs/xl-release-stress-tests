package com.xebialabs.xlrelease.stress.domain

import cats.Show
import cats.implicits._

case class Dependency(dependencyId: Dependency.ID, target: Target)


object Dependency {
  case class ID(taskId: Task.ID, dependency: String)

  object ID {
    implicit val showDependencyId: Show[ID] = {
      case ID(taskId, depId) => taskId.show ++ "/" ++ depId
    }
  }


  implicit val showDependency: Show[Dependency] = {
    case Dependency(id, target) => "{" ++ id.show ++ " -> " ++ target.show ++ "}"
  }
}
package com.xebialabs.xlrelease.stress.domain

import cats.Show

case class Variable(variableId: Variable.ID, value: Option[String])

object Variable {
  case class ID(release: Option[Release.ID], key: String)

  object ID {
    implicit val showVariableID: Show[ID] = {
      case ID(None, key) => "${" ++ key ++ "}"
      case ID(Some(releaseId), key) => releaseId.id ++ "/${" ++ key ++ "}"
    }
  }
}
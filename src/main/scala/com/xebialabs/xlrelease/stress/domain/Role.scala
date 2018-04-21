package com.xebialabs.xlrelease.stress.domain

import cats.Show
import cats.implicits._

case class Role(rolename: Role.ID,
                permissions: Set[Permission],
                principals: Set[User])

object Role {
  type ID = String

  implicit def showRole(implicit showPermission: Show[Permission], showUser: Show[User]): Show[Role] = {
    case Role(name, permissions, principals) =>
      s"$name (permissions: ${permissions.map(_.show).mkString(" ")}) [${principals.map(_.show).mkString(", ")}]"
  }
}

package com.xebialabs.xlrelease.stress.domain

case class Role(rolename: Role.ID,
                permissions: Set[Permission],
                principals: Set[User])

object Role {
  type ID = String


}

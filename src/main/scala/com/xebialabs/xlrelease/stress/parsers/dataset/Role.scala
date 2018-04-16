package com.xebialabs.xlrelease.stress.parsers.dataset

case class Role(rolename: Role.ID,
                permissions: Set[Permission],
                principals: Set[User.ID])

object Role {
  type ID = String
}
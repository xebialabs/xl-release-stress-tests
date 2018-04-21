package com.xebialabs.xlrelease.stress.dsl.xlr

import com.xebialabs.xlrelease.stress.domain.{HttpSession, Role, User}
import freestyle.free._


@free trait Users {
  def login(user: User): FS[HttpSession]
  def admin(): FS[HttpSession]

  def createUser(user: User): FS[User.ID]
  def createRole(role: Role): FS[Role.ID]

  def deleteUser(userId: User.ID): FS[Unit]
  def deleteRole(userId: Role.ID): FS[Unit]
}

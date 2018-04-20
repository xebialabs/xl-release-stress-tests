package com.xebialabs.xlrelease.stress.api.xlr

import com.xebialabs.xlrelease.stress.domain.{HttpSession, Role, User}
import freestyle.free._


@free trait Users {
  def admin(): FS[HttpSession]
  def createUser(user: User): FS[User.ID]
  def login(user: User): FS[HttpSession]
  def createRole(role: Role): FS[Role.ID]

  def deleteUser(id: User.ID): FS[Unit]
  def deleteRole(id: Role.ID): FS[Unit]
}

package com.xebialabs.xlrelease.stress.client

import com.xebialabs.xlrelease.stress.client.akkaClient.HttpSession
import com.xebialabs.xlrelease.stress.domain.{Role, User}
import freestyle.free._


@free trait Users {
  def admin(): FS[HttpSession]
  def createUser(user: User): FS[User.ID]
  def login(user: User): FS[HttpSession]
  def createRole(role: Role): FS[Role.ID]

  def deleteUser(id: User.ID): FS[Unit]
  def deleteRole(id: Role.ID): FS[Unit]
}

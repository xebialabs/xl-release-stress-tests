package com.xebialabs.xlrelease.stress.client

import com.xebialabs.xlrelease.stress.parsers.dataset.User
import freestyle.free._
import freestyle.free.implicits._


@free trait Users {
  def createUser(user: User): FS[User.ID]
  def login(user: User): FS[User.Session]
}

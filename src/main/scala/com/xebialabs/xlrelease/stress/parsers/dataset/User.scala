package com.xebialabs.xlrelease.stress.parsers.dataset

import com.xebialabs.xlrelease.stress.client.akkaClient.HttpSession

case class User(username: User.ID,
                fullname: String,
                email: String,
                password: String)

object User {
  type ID = String
  type Session = HttpSession
}

case class UsersSet(name: String, users: List[User])
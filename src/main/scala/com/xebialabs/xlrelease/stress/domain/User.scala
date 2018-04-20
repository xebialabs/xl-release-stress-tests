package com.xebialabs.xlrelease.stress.domain

case class User(username: User.ID,
                fullname: String,
                email: String,
                password: String)

object User {
  type ID = String
  type Session = HttpSession
}

case class UsersSet(name: String, users: List[User])
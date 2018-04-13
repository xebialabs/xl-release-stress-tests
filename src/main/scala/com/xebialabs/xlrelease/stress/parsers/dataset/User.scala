package com.xebialabs.xlrelease.stress.parsers.dataset

case class User(username: String,
                fullname: String,
                email: String,
                password: String)

object User {
  type ID = String
  // TODO: un-mock
  type Session = String
}

case class UsersSet(name: String, users: List[User])
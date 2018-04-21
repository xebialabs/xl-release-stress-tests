package com.xebialabs.xlrelease.stress.domain

import cats.Show

case class User(username: User.ID,
                fullname: String,
                email: String,
                password: String)

object User {
  type ID = String
  type Session = HttpSession

  implicit val showUser: Show[User] = {
    case User (username, fullname, email, _) =>
      val mail = if (email.isEmpty) "" else s" <$email>"
      val full = if (fullname.isEmpty) "" else s" ($fullname$mail)"
      s"$username$full"
  }
}

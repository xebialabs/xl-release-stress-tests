package com.xebialabs.xlrelease.stress.domain

import akka.http.scaladsl.model.headers.Cookie

case class HttpSession(user: User, cookies: Seq[Cookie])

object HttpSession {
  implicit class HttpSessionOps(val session: HttpSession) extends AnyVal {
    def action[T](act: HttpSession => T): T = act(session)
  }


}
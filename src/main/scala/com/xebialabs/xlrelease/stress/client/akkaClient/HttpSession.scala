package com.xebialabs.xlrelease.stress.client.akkaClient

import akka.http.scaladsl.model.headers.Cookie
import com.xebialabs.xlrelease.stress.parsers.dataset.User

case class HttpSession(user: User, cookies: Seq[Cookie])

object HttpSession {
  implicit class HttpSessionOps(val session: HttpSession) extends AnyVal {
    def action[T](act: HttpSession => T): T = act(session)
  }


}
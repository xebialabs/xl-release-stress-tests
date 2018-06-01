package com.xebialabs.xlrelease.stress.dsl.libs.xlr

import cats._
import cats.implicits._
import com.xebialabs.xlrelease.stress.dsl.Control
import com.xebialabs.xlrelease.stress.dsl.http.{Client, libs}
import com.xebialabs.xlrelease.stress.dsl.http.libs.log.SessionLog
import com.xebialabs.xlrelease.stress.dsl.libs.Api
import com.xebialabs.xlrelease.stress.dsl.libs.control.Until
import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.effects.error.ErrorM
import freestyle.free.logging.LoggingM

trait XlrLib[F[_]] extends Api[F] { self =>
  require(self._api != null)

  object api {
    val http: Client[F] = self._api.http.http
    val log: LoggingM[F] = self._api.http.log
    val error: ErrorM[F] = self._api.http.error
    val control: Control[F] = self._api.control

    def fail[A](msg: String): Program[A] = self._api.http.error.error[A](new RuntimeException(msg))
    def ok[A](a: => A): Program[A] = a.pure[Program]
  }

  object lib extends libs.BaseLib[F](self._api.http) {
    val control: Until[F] = new Until[F]()(self._api)
  }

  val log: SessionLog[F] = lib.log.session
}

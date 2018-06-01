package com.xebialabs.xlrelease.stress.dsl

import cats._
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.XlrConfig
import com.xebialabs.xlrelease.stress.dsl.libs.control.Until
import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.effects.error.ErrorM
import freestyle.free.logging.LoggingM

import scala.concurrent.duration.FiniteDuration


trait API { self =>
  def config: XlrConfig

  type F[A] = Op[A]

  type Program[A] = FreeS[F, A]

  protected def _api: DSL[F]

  object api {
    val low: DSL[F] = _api

    val control: Control[F] = self._api.control

    val log: LoggingM[F] = self._api.http.log
    val error: ErrorM[F] = self._api.http.error

    val xlr = new com.xebialabs.xlrelease.stress.dsl.libs.xlr.Xlr[F](config.server, config.adminPassword)(self._api)

    val lib: http.libs.BaseLib[F] = new http.libs.BaseLib[F](self._api.http) {
      object controls extends Until[F] {
        override protected val _api: DSL[F] = self._api
      }
    }

    def fail[A](msg: String): Program[A] = error.error[A](new RuntimeException(msg))
    def ok[A](a: => A): Program[A] = a.pure[Program]
  }

}

package com.xebialabs.xlrelease.stress.dsl.http.libs.log

import com.xebialabs.xlrelease.stress.domain.User
import com.xebialabs.xlrelease.stress.dsl.http.DSL
import com.xebialabs.xlrelease.stress.dsl.http.libs.Api
import freestyle.free._
import freestyle.free.implicits._

trait SessionLog[F[_]] extends Api[F] { self =>

  private def api: DSL[F] = self._api

  def error(msg: String)(implicit session: User.Session): Program[Unit] = api.log.error(sessionLog(msg))
  def warn(msg: String)(implicit session: User.Session): Program[Unit] = api.log.warn(sessionLog(msg))
  def info(msg: String)(implicit session: User.Session): Program[Unit] = api.log.info(sessionLog(msg))
  def debug(msg: String)(implicit session: User.Session): Program[Unit] = api.log.debug(sessionLog(msg))

  def sessionLog(msg: String)(implicit session: User.Session): String =
    s"${session.user.username}: $msg"
}
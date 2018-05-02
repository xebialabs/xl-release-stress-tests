package com.xebialabs.xlrelease.stress.dsl.libs.xlr

import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.dsl.DSL
import com.xebialabs.xlrelease.stress.dsl.libs.Api


class Xlr[F[_]](server: XlrServer, adminPassword: AdminPassword)(implicit protected val _api: DSL[F]) extends Api[F] { self =>
  require(_api != null)
  val users: Users[F] = new Users[F](server, adminPassword)
  val templates: Templates[F] = new Templates[F](server)
  val releases: Releases[F] = new Releases[F](server)
  val phases: Phases[F] = new Phases[F](server)
  val tasks: Tasks[F] = new Tasks[F](server, phases)
}

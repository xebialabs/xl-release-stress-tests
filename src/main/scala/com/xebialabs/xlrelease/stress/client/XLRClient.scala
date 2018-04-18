package com.xebialabs.xlrelease.stress.client

import freestyle.free._
import freestyle.free.logging._

import scala.language.postfixOps

@module trait XLRClient {
  val users: Users
  val releases: Releases
  val tasks: Tasks

  val log: LoggingM
}


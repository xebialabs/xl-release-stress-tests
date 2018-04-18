package com.xebialabs.xlrelease.stress.client

import freestyle.free._
// (begging) DO NOT REMOVE THIS IMPORT
//import freestyle.free.implicits._

import scala.language.postfixOps

@module trait XLRClient {
  val users: Users
  val releases: Releases
  val tasks: Tasks
}


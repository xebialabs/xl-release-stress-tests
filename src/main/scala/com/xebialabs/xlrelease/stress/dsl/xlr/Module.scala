package com.xebialabs.xlrelease.stress.dsl.xlr

import freestyle.free.module

@module trait Module {
  val users: Users
  val releases: Releases
  val tasks: Tasks
}


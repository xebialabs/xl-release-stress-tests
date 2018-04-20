package com.xebialabs.xlrelease.stress.api.xlr

import freestyle.free.module

@module trait Module {
  val users: Users
  val releases: Releases
  val tasks: Tasks
}


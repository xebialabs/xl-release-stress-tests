package com.xebialabs.xlrelease.stress.api

import freestyle.free.module
import freestyle.free.logging._

@module trait Module {
  val control: com.xebialabs.xlrelease.stress.api.exec.Control
  val xlr: com.xebialabs.xlrelease.stress.api.xlr.Module

  val log: LoggingM
}

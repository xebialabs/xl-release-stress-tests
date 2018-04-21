package com.xebialabs.xlrelease.stress.dsl

import freestyle.free.module
import freestyle.free.logging._

@module trait DSL {
  val control: com.xebialabs.xlrelease.stress.dsl.exec.Control
  val xlr: com.xebialabs.xlrelease.stress.dsl.xlr.Module

  val log: LoggingM
}

package com.xebialabs.xlrelease.stress.dsl.http

import freestyle.free.effects.error.ErrorM
import freestyle.free.logging.LoggingM
import freestyle.free.module

@module trait DSL {
  val http: Client

  val log: LoggingM
  val error: ErrorM
}

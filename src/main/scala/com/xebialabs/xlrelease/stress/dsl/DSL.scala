package com.xebialabs.xlrelease.stress.dsl

import com.xebialabs.xlrelease.stress.dsl.http.{DSL => HttpDSL}
import freestyle.free.module

@module trait DSL {
  val http: HttpDSL
  val control: Control
}

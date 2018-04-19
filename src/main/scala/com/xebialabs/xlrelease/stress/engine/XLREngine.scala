package com.xebialabs.xlrelease.stress.engine

import com.xebialabs.xlrelease.stress.client.XLRClient
import freestyle.free.module

@module trait XLREngine {
  val engine: Engine
  val client: XLRClient
}
package com.xebialabs.xlrelease.stress.dsl

import freestyle.free.{FreeS, module}

package object xlr {
  @module trait Xlr {
    val users: Users
    val releases: Releases
    val tasks: Tasks
  }


  type API = com.xebialabs.xlrelease.stress.dsl.xlr.Xlr[Xlr.Op]
  type Program[A] = FreeS[Xlr.Op, A]
}

package com.xebialabs.xlrelease.stress.dsl

import freestyle.free.FreeS

package object xlr {
  type API = com.xebialabs.xlrelease.stress.dsl.xlr.Module[Module.Op]
  type Program[A] = FreeS[Module.Op, A]
}

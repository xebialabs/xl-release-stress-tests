package com.xebialabs.xlrelease.stress.api

import freestyle.free.FreeS

package object xlr {
  type API = com.xebialabs.xlrelease.stress.api.xlr.Module[Module.Op]
  type Program[A] = FreeS[Module.Op, A]
}

package com.xebialabs.xlrelease.stress

import freestyle.free.FreeS

package object api {
  type API = Module[Module.Op]
  type Program[A] = FreeS[Module.Op, A]
}

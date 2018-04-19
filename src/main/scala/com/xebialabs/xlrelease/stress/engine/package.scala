package com.xebialabs.xlrelease.stress

import com.xebialabs.xlrelease.stress.client.XLRClient
import freestyle.free.FreeS

package object engine {
  type API = XLREngine[XLREngine.Op]
  type HiProgram[A] = FreeS[XLREngine.Op, A]
}

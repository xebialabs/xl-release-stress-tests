package com.xebialabs.xlrelease.stress

import com.xebialabs.xlrelease.stress.client.XLRClient
import freestyle.free.FreeS

package object engine {
  type Program[A] = FreeS[XLRClient.Op, A]

}

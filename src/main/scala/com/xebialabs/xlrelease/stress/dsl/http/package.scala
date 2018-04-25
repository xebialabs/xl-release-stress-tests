package com.xebialabs.xlrelease.stress.dsl

import freestyle.free.FreeS

package object http {
  type Program[A] = FreeS[DSL.Op, A]
}

package com.xebialabs.xlrelease.stress

import freestyle.free.FreeS

package object dsl {
  type Program[A] = FreeS[DSL.Op, A]

  type Op[A] = DSL.Op[A]
}

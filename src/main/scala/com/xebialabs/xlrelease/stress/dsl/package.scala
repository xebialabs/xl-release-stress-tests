package com.xebialabs.xlrelease.stress

import cats.implicits._
import freestyle.free.FreeS

package object dsl {
  type API = DSL[DSL.Op]
  type Program[A] = FreeS[DSL.Op, A]

  val nop: Program[Unit] = ().pure[Program]
}

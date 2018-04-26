package com.xebialabs.xlrelease.stress.dsl.libs

import com.xebialabs.xlrelease.stress.dsl.DSL
import freestyle.free.FreeS


trait Api[F[_]] {
  protected def _api: DSL[F]

  type Program[A] = FreeS[F, A]
}

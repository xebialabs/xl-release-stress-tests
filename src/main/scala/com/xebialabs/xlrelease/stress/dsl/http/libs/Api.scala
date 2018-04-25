package com.xebialabs.xlrelease.stress.dsl.http.libs

import com.xebialabs.xlrelease.stress.dsl.http.DSL
import freestyle.free.FreeS


trait Api[F[_]] {
  protected def _api: DSL[F]

  type Program[A] = FreeS[F, A]
}

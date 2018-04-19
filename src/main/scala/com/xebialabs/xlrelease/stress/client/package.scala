package com.xebialabs.xlrelease.stress

import cats.{Applicative, Monad, Parallel, ~>}
import cats.free.{Free, FreeApplicative}
import freestyle.free.FreeS

package object client {
  type API = XLRClient[XLRClient.Op]
  type Program[A] = FreeS[XLRClient.Op, A]

  implicit def catsFreeParallelForFreeFreeApplicative[F[_], G[_]](implicit FG: Parallel[F, G]): Parallel[Free[F, ?], FreeApplicative[G, ?]] =
    new Parallel[Free[F, ?], FreeApplicative[G, ?]] {
      val parallel: Free[F, ?] ~> FreeApplicative[G, ?] =
        λ[Free[F, ?] ~> FreeApplicative[G, ?]](fa =>
          FreeApplicative.lift(FG.parallel(fa.runTailRec(FG.monad))))

      val sequential: FreeApplicative[G, ?] ~> Free[F, ?] =
        λ[FreeApplicative[G, ?] ~> Free[F, ?]](fa =>
          Free.liftF(FG.sequential(fa.fold(FG.applicative))))

      val applicative: Applicative[FreeApplicative[G, ?]] = implicitly

      val monad: Monad[Free[F, ?]] = implicitly
    }
}

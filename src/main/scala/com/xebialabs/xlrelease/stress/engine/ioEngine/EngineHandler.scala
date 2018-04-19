package com.xebialabs.xlrelease.stress.engine.ioEngine

import cats.implicits._
import cats.effect.IO
import com.xebialabs.xlrelease.stress.client.{XLRClient, Program}
import com.xebialabs.xlrelease.stress.client.akkaClient.Runner.runIO
import com.xebialabs.xlrelease.stress.client.akkaClient.AkkaHttpXlrClient
import com.xebialabs.xlrelease.stress.engine.Engine


class EngineHandler(implicit client: AkkaHttpXlrClient, API: XLRClient[XLRClient.Op]) {

  implicit def engineHandler: Engine.Handler[IO] = new Engine.Handler[IO] {
    protected[this] def par[A, B](p1: Program[A], p2: Program[B]): IO[(A, B)] = {
      (runIO(p1), runIO(p2)).mapN((a, b) => (a, b))
    }

    protected[this] def parallel[A](n: Int)(p: Int => Program[A]): IO[List[A]] =
      (0 to n).toList
        .par
          .map(p andThen runIO)
        .seq.toList
        .sequence

  }
}
package com.xebialabs.xlrelease.stress.engine.ioEngine

import java.util.concurrent.Executors

import cats.implicits._
import cats.effect.IO
import com.xebialabs.xlrelease.stress.client.{Program, XLRClient}
import com.xebialabs.xlrelease.stress.client.akkaClient.Runner.runIO
import com.xebialabs.xlrelease.stress.client.akkaClient.AkkaHttpXlrClient
import com.xebialabs.xlrelease.stress.engine.Engine

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}


class EngineHandler(implicit client: AkkaHttpXlrClient, API: XLRClient[XLRClient.Op], ec: ExecutionContext) {


  implicit def engineHandler: Engine.Handler[IO] = new Engine.Handler[IO] {
    protected[this] def par[A, B](p1: Program[A], p2: Program[B]): IO[(A, B)] = {
      (runIO(p1), runIO(p2)).mapN((a, b) => (a, b))
    }

    protected[this] def parallel[A](n: Int)(p: Int => Program[A]): IO[List[A]] =
      IO.pure {
        (0 to n)
          .map(p)
          .par
            .map(program => runIO(program).unsafeRunSync())
          .seq.toList
      }
  }
}
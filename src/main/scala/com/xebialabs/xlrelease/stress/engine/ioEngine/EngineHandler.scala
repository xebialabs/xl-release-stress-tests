package com.xebialabs.xlrelease.stress.engine.ioEngine


import cats.implicits._
import cats.effect.IO
import com.xebialabs.xlrelease.stress.client.{Program, XLRClient}
import com.xebialabs.xlrelease.stress.client.akkaClient.Runner.runIO
import com.xebialabs.xlrelease.stress.client.akkaClient.AkkaHttpXlrClient
import com.xebialabs.xlrelease.stress.engine.Engine

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext


class EngineHandler(implicit client: AkkaHttpXlrClient, API: XLRClient[XLRClient.Op], ec: ExecutionContext) {


  implicit def engineHandler: Engine.Handler[IO] = new Engine.Handler[IO] {

    protected def parallel[A](n: Int)(p: Int => Program[A]): IO[List[A]] =
      IO.pure {
        (0 until n)
          .map(p)
          .par
            .map(program => runIO(program).unsafeRunSync())
          .seq.toList
      }

    protected def pause(duration: Duration): IO[Unit] = {
      Thread.sleep(duration.toMillis)
      IO.pure(())
    }
  }
}
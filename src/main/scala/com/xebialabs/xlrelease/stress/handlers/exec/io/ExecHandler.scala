package com.xebialabs.xlrelease.stress.handlers.exec.io


import cats.implicits._
import cats.effect.IO
import com.xebialabs.xlrelease.stress.Runner.runIO
import com.xebialabs.xlrelease.stress.api.{API, Program}
import com.xebialabs.xlrelease.stress.handlers.xlr.akkaClient.AkkaHttpXlrClient
import com.xebialabs.xlrelease.stress.api.exec.Control

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext


class ExecHandler(implicit client: AkkaHttpXlrClient, API: API, ec: ExecutionContext) {

  implicit def engineHandler: Control.Handler[IO] = new Control.Handler[IO] {

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
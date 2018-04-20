package com.xebialabs.xlrelease.stress.handlers.io.exec


import cats.effect.IO
import com.xebialabs.xlrelease.stress.api.{API, Program}
import com.xebialabs.xlrelease.stress.http.AkkaHttpClient
import com.xebialabs.xlrelease.stress.api.exec.Control
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.handlers.io.Runner

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext


class ControlHandler(implicit
                     server: XlrServer,
                     admin: AdminPassword,
                     client: AkkaHttpClient,
                     API: API,
                     ec: ExecutionContext) extends Runner {

  implicit def controlHandler: Control.Handler[IO] = new Control.Handler[IO] {

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
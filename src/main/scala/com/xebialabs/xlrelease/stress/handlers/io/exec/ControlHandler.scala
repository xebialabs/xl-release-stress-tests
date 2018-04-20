package com.xebialabs.xlrelease.stress.handlers.io.exec


import cats.effect.IO
import com.xebialabs.xlrelease.stress.Runner
import com.xebialabs.xlrelease.stress.api.{API, Program}
import com.xebialabs.xlrelease.stress.handlers.akkaClient.AkkaHttpXlrClient
import com.xebialabs.xlrelease.stress.api.exec.Control
import com.xebialabs.xlrelease.stress.domain.{AdminPassword, XlrServer}

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext


class ControlHandler(implicit
                     server: XlrServer,
                     admin: AdminPassword,
                     client: AkkaHttpXlrClient,
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
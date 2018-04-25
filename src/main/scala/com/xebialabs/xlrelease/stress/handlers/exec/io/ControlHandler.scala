package com.xebialabs.xlrelease.stress.handlers.exec.io

import cats.effect.IO
import com.xebialabs.xlrelease.stress.dsl.Program
import com.xebialabs.xlrelease.stress.dsl.exec.Control
import com.xebialabs.xlrelease.stress.runners.io.{RunnerContext, runIO}

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}


object ControlHandler {

  implicit def controlHandler(implicit ctx: RunnerContext): Control.Handler[IO] = new Control.Handler[IO] {

    protected def parallel[A](n: Int)(p: Int => Program[A]): IO[List[A]] =
      IO.pure {
        (0 until n)
          .map(p)
          .par
            .map(program => Try(runIO(program).unsafeRunSync()) match {
              case Success(a) =>
                Some(a)
              case Failure(err) =>
                err.printStackTrace()
                None
            })
          .seq.toList.flatten
      }

    protected def pause(duration: Duration): IO[Unit] = {
      Thread.sleep(duration.toMillis)
      IO.pure(())
    }

    protected def ok[A](value: A): IO[A] =
      IO.pure(value)

    protected def error[A](error: Throwable): IO[A] =
      IO.raiseError(error)
  }
}

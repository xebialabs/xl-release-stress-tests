package com.xebialabs.xlrelease.stress.handlers.io


import cats.implicits._
import cats.effect.IO
import com.xebialabs.xlrelease.stress.dsl
import freestyle.free._
import freestyle.free.implicits._
import org.joda.time.{DateTime, Duration}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration


class ControlHandler()(implicit httpClientHandler: dsl.http.Client.Handler[IO]) {
  import freestyle.free.loggingJVM.log4s.implicits._
  import freestyle.free.effects.error.implicits._

  implicit def controlHandler(implicit ec: ExecutionContext): dsl.Control.Handler[IO] = new dsl.Control.Handler[IO] {
    protected def sleep(duration: FiniteDuration): IO[Unit] =
      IO.sleep(duration)

    protected def fork[A, B](left: dsl.Program[A], right: dsl.Program[B]): IO[(A, B)] =
      for {
        l <- IO.shift *> left.interpret[IO].start
        r <- IO.shift *> right.interpret[IO].start
        a <- l.join
        b <- r.join
      } yield (a, b)

    protected def repeat[A](n: Int)(program: dsl.Program[A]): IO[List[A]] = {
      val code: IO[A] = program.interpret[IO]
      (0 until n).toList
        .map(_ => code.start >>= (_.join))
        .sequence
    }

    protected def parallel[A](n: Int)(p: Int => dsl.Program[A]): IO[List[A]] =
      (0 until n).toList
        .map(i => p(i).interpret[IO])
        .map(code => IO.shift *> code.start)
        .parTraverse(async => async >>= (_.join))

    protected def now(): IO[DateTime] =
      IO(DateTime.now)

    protected def time[A](p: dsl.Program[A]): IO[(Duration, A)] =
      for {
        start <- now()
        result <- p.interpret[IO]
        end <- now()
        duration = new Duration(start, end)
      } yield duration -> result
  }
}

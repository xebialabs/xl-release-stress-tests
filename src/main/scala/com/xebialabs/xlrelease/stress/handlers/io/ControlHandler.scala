package com.xebialabs.xlrelease.stress.handlers.io


import java.util.concurrent.atomic.AtomicBoolean

import cats.implicits._
import cats.effect.IO
import com.xebialabs.xlrelease.stress.dsl
import freestyle.free._
import freestyle.free.implicits._
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.control.NonFatal


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

    protected def backgroundOf[A, B](foreground: dsl.Program[A])(background: dsl.Program[B]): IO[(A, List[B])] = {
      val fg: IO[A] = IO.shift *> foreground.interpret[IO]
      val bg: IO[B] = background.interpret[IO]

      var acc = List.empty[B]
      val stop = new AtomicBoolean(false)

      def bgLoop: IO[List[B]] = IO.suspend {
        if (stop.get()) {
          IO.pure(acc)
        } else {
          val next = for {
            b <- bg
            _ <- IO {
              acc = b :: acc
            }
            res <- bgLoop
          } yield res
          IO.cancelBoundary *> next
        }
      }

      for {
        fa <- IO.shift *> fg.start
        fb <- IO.shift *> bgLoop.start
        a <- fa.join
        _ <- IO {
          stop.set(true)
        }
        bs <- fb.join
      } yield (a, bs)
    }

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

    protected def time[A](p: dsl.Program[A]): IO[(FiniteDuration, A)] =
      for {
        start <- now()
        result <- p.interpret[IO]
        end <- now()
        duration = new FiniteDuration(end.getMillis - start.getMillis, MILLISECONDS)
      } yield duration -> result
  }
}

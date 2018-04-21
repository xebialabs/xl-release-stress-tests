package com.xebialabs.xlrelease.stress.http

import cats._
import cats.implicits._
import freestyle.free._
import freestyle.free.implicits._

import scala.concurrent.duration.FiniteDuration

class HttpLib[F[_]]()(implicit api: Http[F]) {
  type Target[A] = FreeS[F, A]

  val client = new ClientLib[F]()(api.client)

  def error[A](msg: String): Target[A] =
    for {
      _ <- api.log.error(msg)
      res <- api.error.error[A](new RuntimeException(msg))
    } yield res

  def until[A](cond: A => Boolean, interval: FiniteDuration, retries: Option[Int])
              (get: Target[A]): Target[A] = {
    require(retries.fold(true)(_ > 0))

    def loop(attempts: Int): Target[A] = retries match {
      case Some(n) if attempts > n =>
        error(s"Exceeded allowed $n retries. Giving up")
      case _ =>
        get >>= { found =>
          if (cond(found))
            found.pure[Target]
          else
            loop(attempts + 1)
        }
    }

    loop(attempts = 1)
  }
}


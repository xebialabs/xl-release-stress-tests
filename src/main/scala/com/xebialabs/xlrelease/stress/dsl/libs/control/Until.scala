package com.xebialabs.xlrelease.stress.dsl.libs.control

import cats._
import cats.implicits._
import com.xebialabs.xlrelease.stress.dsl.DSL
import com.xebialabs.xlrelease.stress.dsl.libs.Api
import freestyle.free._
import freestyle.free.implicits._

import scala.concurrent.duration.FiniteDuration

class Until[F[_]]()(implicit protected val _api: DSL[F]) extends Api[F] { self =>
  private val api: DSL[F] = self._api

  def until[A](cond: A => Boolean, interval: FiniteDuration, retries: Option[Int])
              (get: Program[A]): Program[A] = {
    require(retries.fold(true)(_ > 0))

    def loop(attempts: Int): Program[A] = {
      api.http.log.debug(s"loop($attempts/${retries.fold("-")(_.toString)}) every $interval")
      retries match {
        case Some(n) if attempts > n =>
          api.http.error.error[A](new RuntimeException(s"Exceeded allowed $n retries. Giving up"))
        case _ =>
          get >>= { found =>
            if (cond(found))
              found.pure[Program]
            else
              for {
                _ <- api.control.sleep(interval)
                r <- loop(attempts + 1)
              } yield r
          }
      }
    }

    loop(attempts = 1)
  }
}

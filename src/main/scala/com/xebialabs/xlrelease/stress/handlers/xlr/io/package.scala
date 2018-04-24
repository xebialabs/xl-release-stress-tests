package com.xebialabs.xlrelease.stress.handlers.xlr

import akka.http.scaladsl.model.Uri.Path./
import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.stream.Materializer
import akka.util.Timeout
import cats.effect.IO
import cats.implicits._
import com.xebialabs.xlrelease.stress.domain.{HttpSession, Task}
import com.xebialabs.xlrelease.stress.handlers.http.future._
import com.xebialabs.xlrelease.stress.utils.JsUtils.JsParsed
import spray.json.{DefaultJsonProtocol, DeserializationException, JsValue}

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

package object io {

  implicit class FutureHttpResponseOps(val futureResponse: Future[HttpResponse]) extends DefaultJsonProtocol {
    def asJson(implicit ec: ExecutionContext, m: Materializer): Future[JsValue] = futureResponse.flatMap(_.entity.asJson[JsValue])

    def discard[A](f: HttpResponse => A)(implicit ec: ExecutionContext, m: Materializer): Future[A] = futureResponse.map { resp =>
      val a = f(resp)
      resp.discardEntityBytes()
      a
    }

    def discardU(implicit ec: ExecutionContext, m: Materializer): Future[Unit] = discard(_ => ())
  }

  def doUntil[A](cond: A => Boolean, interval: FiniteDuration, retries: Option[Int])
                (get: () => IO[A])
                (implicit ec: ExecutionContext): IO[Unit] = {
    @tailrec
    def loop(attempts: Int): IO[Unit] =
      retries match {
        case Some(n) if attempts >= n =>
          IO.raiseError(new RuntimeException(s"Reached retries limit ($n), giving up."))
        case _ =>
          val found = get().unsafeRunSync()
          if (cond(found)) {
            IO.pure(())
          } else {
            IO.sleep(interval).unsafeRunSync()
            loop(attempts + 1)
          }
      }

    loop(0)
  }

  implicit class DoIOOps[A](val doIO: () => IO[A]) extends AnyVal {
    def until(cond: A => Boolean, interval: FiniteDuration, retries: Option[Int] = None)
             (implicit ec: ExecutionContext): IO[Unit] =
      doUntil(cond, interval, retries)(doIO)
  }

  def doUntil[A](cond: A => Boolean, interval: Duration, retries: Option[Int] = None)
                (get: () => Future[A])
                (implicit timeout: Timeout): Future[Unit] = {
    @tailrec
    def loop(attempts: Int): Future[Unit] = {
      retries match {
        case Some(n) if attempts >= n =>
          Future.failed(new RuntimeException(s"Ran out of retries after $attempts attempts"))
        case _ =>
          val found = Await.result(get(), timeout.duration)
          if (cond(found)) {
            Future.successful(())
          } else {
            Thread.sleep(interval.toMillis)
            loop(attempts + 1)
          }
      }
    }
    loop(0)
  }

  implicit class DoFutureOps[A](val doFuture: () => Future[A]) extends AnyVal {
    def until(cond: A => Boolean, interval: Duration, retries: Option[Int] = None)(implicit timeout: Timeout): Future[Unit] =
      doUntil(cond, interval, retries)(doFuture)
  }

  implicit class FutureOps[A](val future: Future[A]) extends AnyVal {
    def io: IO[A] = IO.fromFuture(IO(future))
  }

  implicit class JsParsedOps[A, B](val toJsParsed: A => JsParsed[B]) extends AnyVal {
    def toIO: A => IO[B] = a => IO.fromEither(toJsParsed(a))

    def toIO(msg: String, fields: List[String] = Nil)(implicit session: HttpSession): A => IO[B] = a => IO.fromEither(
      toJsParsed(a).leftMap { err =>
        DeserializationException(s"(${session.user.username}) $msg", err, fields)
      }
    )
  }

  implicit class TaskIdOps(val taskId: Task.ID) extends AnyVal {
    def path: Uri.Path = / / taskId.release / taskId.phase / taskId.task
  }
}

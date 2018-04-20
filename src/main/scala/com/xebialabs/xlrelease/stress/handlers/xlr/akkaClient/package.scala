package com.xebialabs.xlrelease.stress.handlers.xlr

import java.io.ByteArrayOutputStream

import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import cats.effect.IO
import cats.data._
import cats.implicits._
import com.xebialabs.xlrelease.stress.domain.HttpSession
import com.xebialabs.xlrelease.stress.utils.JsUtils.JsParsed
import spray.json._

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps


package object akkaClient {

  implicit class EntityOps(val entity: HttpEntity) extends AnyVal {
    def asByteArray(implicit m: Materializer, ec: ExecutionContext): Future[Array[Byte]] = {
      for {
        outStream <- entity.dataBytes.runWith(
          Sink.fold(new ByteArrayOutputStream()) { (out, bs) =>
            out.write(bs.toArray, 0, bs.length)
            out
          }
        )
      } yield {
        try {
          outStream.toByteArray
        } finally {
          outStream.close()
        }
      }
    }

    def asString(implicit m: Materializer, ex: ExecutionContext): Future[String] =
      entity.dataBytes
        .map(_.utf8String)
        .runWith(Sink.seq)
        .map(_.mkString(""))


    def asJson[T: JsonReader](implicit m: Materializer, ec: ExecutionContext): Future[T] =
      asString
        .map(_.parseJson.convertTo[T])
  }


  implicit class RespSyntax(val resp: HttpResponse) extends AnyVal {
    def onSuccess[A](f: HttpEntity => Future[A]): SuccessMapped[A] = {
      if (resp.status.isFailure()) {
        IsFailed(resp)
      } else {
        OnSuccess(f(resp.entity))
      }
    }
  }

  sealed trait SuccessMapped[+A] {
    def onFailure[E <: Throwable](err: (Int, String) => E)(implicit ec: ExecutionContext, m: Materializer): Future[A] = this match {
      case OnSuccess(resp) => resp
      case IsFailed(resp) => Future.failed(err(resp.status.intValue(), Await.result(resp.entity.asString, 10 seconds)))
    }
  }

  case class OnSuccess[A](value: Future[A]) extends SuccessMapped[A]

  case class IsFailed(resp: HttpResponse) extends SuccessMapped[Nothing]

  implicit class FutureHttpResponseOps(val futureResponse: Future[HttpResponse]) extends DefaultJsonProtocol {
    def asJson(implicit ec: ExecutionContext, m: Materializer): Future[JsValue] = futureResponse.flatMap(_.entity.asJson[JsValue])

    def discard[A](f: HttpResponse => A)(implicit ec: ExecutionContext, m: Materializer): Future[A] = futureResponse.map { resp =>
      val a = f(resp)
      resp.discardEntityBytes()
      a
    }

    def discardU(implicit ec: ExecutionContext, m: Materializer): Future[Unit] = discard(_ => ())
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

//  implicit class OptionOps[A](val option: Option[A]) extends AnyVal {
//    def toIO(ifEmpty: => Throwable): IO[A] = option.map(IO.pure).getOrElse(IO.raiseError(ifEmpty))
//  }

}

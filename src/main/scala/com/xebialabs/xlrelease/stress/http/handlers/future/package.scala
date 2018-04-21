package com.xebialabs.xlrelease.stress.http.handlers

import java.io.ByteArrayOutputStream

import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps


package object future {

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


//  implicit class OptionOps[A](val option: Option[A]) extends AnyVal {
//    def toIO(ifEmpty: => Throwable): IO[A] = option.map(IO.pure).getOrElse(IO.raiseError(ifEmpty))
//  }

}

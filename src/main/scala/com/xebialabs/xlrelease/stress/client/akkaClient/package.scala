package com.xebialabs.xlrelease.stress.client

import java.io.ByteArrayOutputStream

import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
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
}

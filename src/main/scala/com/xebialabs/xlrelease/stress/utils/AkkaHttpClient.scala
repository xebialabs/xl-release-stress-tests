package com.xebialabs.xlrelease.stress.utils

import java.io.{ByteArrayOutputStream, File}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.MediaTypes.{`application/json`, `application/zip`}
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{DateTime => _, _}
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, Materializer}
import cats.effect.IO
import com.xebialabs.xlrelease.stress.domain.HttpSession
import com.xebialabs.xlrelease.stress.config.defaults.http.client.{headers => defaultHeaders}
import spray.json._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class AkkaHttpClient extends SprayJsonSupport with DefaultJsonProtocol {
  import AkkaHttpClient._

  private[this] val logger = org.log4s.getLogger("HTTP")

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  def get(uri: Uri, headers: List[HttpHeader] = defaultHeaders): Future[HttpResponse] =
    request(HttpRequest(GET, uri, headers))

  def post(uri: Uri, entity: RequestEntity, headers: List[HttpHeader] = defaultHeaders): Future[HttpResponse] =
    request(HttpRequest(POST, uri, headers, entity))

  def put(uri: Uri, entity: RequestEntity, headers: List[HttpHeader] = defaultHeaders): Future[HttpResponse] =
    request(HttpRequest(PUT, uri, headers, entity))

  def delete(uri: Uri, headers: List[HttpHeader] = defaultHeaders): Future[HttpResponse] =
    request(HttpRequest(DELETE, uri, headers))

  def request(req: HttpRequest): Future[HttpResponse] =
    Http().singleRequest(req).map { resp =>
      logger.debug(req.method.value + " " + req.uri.toString + ": " + resp.status.toString)
      resp
    }

  def shutdown(): Future[Unit] =
    Http().shutdownAllConnectionPools()

}

object AkkaHttpClient {

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
        .flatMap { content =>
          Try(content.parseJson.convertTo[T]) match {
            case Success(json) => Future.successful(json)
            case Failure(err) =>
              println(s"cannot parse json:")
              println(content)
              err.printStackTrace()
              Future.failed(err)
          }
        }
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

}

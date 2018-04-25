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
import com.xebialabs.xlrelease.stress.domain.HttpSession
import com.xebialabs.xlrelease.stress.config.defaults.http.client.{headers => defaultHeaders}
import spray.json._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class AkkaHttpClient extends SprayJsonSupport with DefaultJsonProtocol {
  import AkkaHttpClient._

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  def get(uri: Uri, headers: List[HttpHeader] = defaultHeaders): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(GET, uri, headers))

  def post(uri: Uri, entity: RequestEntity, headers: List[HttpHeader] = defaultHeaders): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(POST, uri, headers, entity))

  def put(uri: Uri, entity: RequestEntity, headers: List[HttpHeader] = defaultHeaders): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(PUT, uri, headers, entity))

  def getJSON(uri: Uri)(implicit session: HttpSession): Future[JsValue] =
    Http().singleRequest(HttpRequest(GET, uri, headers = Accept(`application/json`) :: session.cookies.toList))
      .flatMap(_.entity.asJson[JsValue])

  def postJSON0(uri: Uri, entity: JsValue, headers: List[HttpHeader] = List(Accept(`application/json`, MediaRanges.`*/*`))): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(POST, uri,
      entity = HttpEntity(`application/json`, entity.compactPrint),
      headers = headers
    ))

  def postJSON(uri: Uri, entity: JsValue)(implicit session: HttpSession): Future[HttpResponse] =
    postJSON0(uri, entity, Accept(`application/json`) :: session.cookies.toList)

  def putJSON(uri: Uri, entity: JsValue)(implicit session: HttpSession): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(PUT, uri,
      entity = HttpEntity(`application/json`, entity.compactPrint),
      headers = Accept(`application/json`) :: session.cookies.toList
    ))
  }

  def postZip(uri: Uri, content: File)(implicit session: HttpSession): Future[HttpResponse] = {
    val payload = Multipart.FormData(
      Multipart.FormData.BodyPart.fromFile(name = "file", `application/zip`, content)
    )
    Http().singleRequest(HttpRequest(POST, uri,
      entity = payload.toEntity(),
      headers = Accept(`application/json`) :: session.cookies.toList
    ))
  }

  def delete(uri: Uri, headers: List[HttpHeader] = defaultHeaders)(implicit session: HttpSession): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(DELETE, uri,
      headers = Accept(`application/json`) :: session.cookies.toList
    ))
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

}
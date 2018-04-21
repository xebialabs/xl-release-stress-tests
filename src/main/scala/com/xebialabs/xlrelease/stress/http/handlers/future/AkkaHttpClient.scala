package com.xebialabs.xlrelease.stress.http.handlers.future

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.MediaTypes.{`application/json`, `application/zip`}
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{DateTime => _, _}
import akka.stream.ActorMaterializer
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.http.defaultHeaders
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class AkkaHttpClient extends SprayJsonSupport with DefaultJsonProtocol {

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
    Http().singleRequest(HttpRequest(GET, uri, headers = Accept(`application/json`) :: session.cookies))
      .flatMap(_.entity.asJson[JsValue])

  def postJSON0(uri: Uri, entity: JsValue, headers: List[HttpHeader] = List(Accept(`application/json`))): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(POST, uri,
      entity = HttpEntity(`application/json`, entity.compactPrint),
      headers = headers
    ))

  def postJSON(uri: Uri, entity: JsValue)(implicit session: HttpSession): Future[HttpResponse] =
    postJSON0(uri, entity, Accept(`application/json`) :: session.cookies)

  def putJSON(uri: Uri, entity: JsValue)(implicit session: HttpSession): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(PUT, uri,
      entity = HttpEntity(`application/json`, entity.compactPrint),
      headers = Accept(`application/json`) :: session.cookies
    ))
  }

  def postZip(uri: Uri, content: File)(implicit session: HttpSession): Future[HttpResponse] = {
    val payload = Multipart.FormData(
      Multipart.FormData.BodyPart.fromFile(name = "file", `application/zip`, content)
    )
    Http().singleRequest(HttpRequest(POST, uri,
      entity = payload.toEntity(),
      headers = Accept(`application/json`) :: session.cookies
    ))
  }

  def delete(uri: Uri, headers: List[HttpHeader] = defaultHeaders)(implicit session: HttpSession): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(DELETE, uri,
      headers = Accept(`application/json`) :: session.cookies
    ))
  }

  def shutdown(): Future[Unit] =
    Http().shutdownAllConnectionPools()

}

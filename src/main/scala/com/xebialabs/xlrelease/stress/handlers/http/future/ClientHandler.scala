package com.xebialabs.xlrelease.stress.handlers.http.future

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpHeader, HttpResponse, RequestEntity, Uri}
import com.xebialabs.xlrelease.stress.dsl.http.Client
import com.xebialabs.xlrelease.stress.handlers.http
import spray.json._

import scala.concurrent.Future

class ClientHandler {
  val client = new AkkaHttpClient

  implicit def clientHandler: Client.Handler[Future] = new Client.Handler[Future] with SprayJsonSupport with DefaultJsonProtocol {
    protected def get(uri: Uri, headers: List[HttpHeader]): Future[HttpResponse] =
      client.get(uri, headers)

    protected def post(uri: Uri, entity: RequestEntity, headers: List[HttpHeader]): Future[HttpResponse] =
      client.post(uri, entity, headers)

    protected def put(uri: Uri, entity: RequestEntity, headers: List[HttpHeader]): Future[HttpResponse] =
      client.put(uri, entity, headers)

    protected def delete(uri: Uri, headers: List[HttpHeader]): Future[HttpResponse] =
      client.delete(uri, headers)(null)

    protected def parseJson(resp: HttpResponse): Future[JsValue] = {
      import client.materializer
      import client.ec
      resp.entity.asJson[JsValue]
    }

    protected def discard(resp: HttpResponse): Future[Unit] = {
      import client.materializer
      resp.discardEntityBytes()
      Future.successful(())
    }
  }
}

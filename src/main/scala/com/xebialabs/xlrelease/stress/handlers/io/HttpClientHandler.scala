package com.xebialabs.xlrelease.stress.handlers.io

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpHeader, HttpResponse, RequestEntity, Uri}
import cats.effect.IO
import cats.implicits._
import com.xebialabs.xlrelease.stress.dsl.http.Client
import com.xebialabs.xlrelease.stress.utils.AkkaHttpClient
import com.xebialabs.xlrelease.stress.utils.AkkaHttpClient.EntityOps
import spray.json._

import scala.util.Try

class HttpClientHandler() {
  val client = new AkkaHttpClient
  import client.materializer

  implicit def clientHandler: Client.Handler[IO] = new Client.Handler[IO] with SprayJsonSupport with DefaultJsonProtocol {
    protected def get(uri: Uri, headers: List[HttpHeader]): IO[HttpResponse] =
      client.get(uri, headers).io

    protected def post(uri: Uri, entity: RequestEntity, headers: List[HttpHeader]): IO[HttpResponse] =
      client.post(uri, entity, headers).io

    protected def put(uri: Uri, entity: RequestEntity, headers: List[HttpHeader]): IO[HttpResponse] =
      client.put(uri, entity, headers).io

    protected def delete(uri: Uri, headers: List[HttpHeader]): IO[HttpResponse] =
      client.delete(uri, headers)(null).io

    protected def parseJson(resp: HttpResponse): IO[JsValue] = {
      import client.{ec, materializer}
      for {
        content <- resp.entity.asString.io
        json <- IO.fromEither(Try(content.parseJson).fold(_.asLeft, _.asRight))
      } yield json
    }

    protected def discard(resp: HttpResponse): IO[Unit] = {
      resp.discardEntityBytes()
      ().pure[IO]
    }
  }
}
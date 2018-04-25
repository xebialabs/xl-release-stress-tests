package com.xebialabs.xlrelease.stress.dsl.http

import akka.http.scaladsl.model.{HttpHeader, HttpResponse, RequestEntity, Uri}
import com.xebialabs.xlrelease.stress.config.defaults.http.client.{headers => defaultHeaders}
import freestyle.free._
import spray.json.JsValue

@free trait Client {
  def get(uri: Uri, headers: List[HttpHeader] = defaultHeaders): FS[HttpResponse]
  def post(uri: Uri, entity: RequestEntity, headers: List[HttpHeader] = defaultHeaders): FS[HttpResponse]
  def put(uri: Uri, entity: RequestEntity, headers: List[HttpHeader] = defaultHeaders): FS[HttpResponse]
  def delete(uri: Uri, headers: List[HttpHeader] = defaultHeaders): FS[HttpResponse]

  def parseJson(resp: HttpResponse): FS[JsValue]
  def discard(resp: HttpResponse): FS[Unit]
}

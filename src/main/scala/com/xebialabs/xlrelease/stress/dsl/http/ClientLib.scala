package com.xebialabs.xlrelease.stress.dsl.http

import java.io.File

import akka.http.scaladsl.model.MediaTypes.{`application/json`, `application/zip`}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import com.xebialabs.xlrelease.stress.domain.HttpSession
import freestyle.free._
import spray.json._

class ClientLib[F[_]]()(implicit api: Client[F]) {
  type Target[A] = FreeS[F, A]

  def postJSON0(uri: Uri, entity: JsValue, headers: List[HttpHeader] = defaultHeaders): Target[HttpResponse] =
    api.post(uri, jsonEntity(entity), headers)

  def putJSON0(uri: Uri, entity: JsValue, headers: List[HttpHeader] = defaultHeaders): Target[HttpResponse] =
    api.put(uri, jsonEntity(entity), headers)

  def postZip0(uri: Uri, content: File, headers: List[HttpHeader] = defaultHeaders): Target[HttpResponse] = {
    val payload = Multipart.FormData(
      Multipart.FormData.BodyPart.fromFile(name = "file", `application/zip`, content)
    )
    api.post(uri, payload.toEntity(), headers)
  }

  def postJSON(uri: Uri, entity: JsValue, headers: List[HttpHeader] = defaultHeaders)
              (implicit session: HttpSession): Target[HttpResponse] =
    postJSON(uri, entity, Accept(`application/json`) :: session.cookies.toList)

  def putJSON(uri: Uri, entity: JsValue, headers: List[HttpHeader] = defaultHeaders)
             (implicit session: HttpSession): Target[HttpResponse] =
    putJSON(uri, entity, headers ++ session.cookies.toList)

  def postZip(uri: Uri, content: File, headers: List[HttpHeader] = defaultHeaders)
             (implicit session: HttpSession): Target[HttpResponse] =
    postZip(uri, content, headers ++ session.cookies.toList)

  def delete(uri: Uri, headers: List[HttpHeader] = defaultHeaders)
            (implicit session: HttpSession): Target[HttpResponse] =
    api.delete(uri, headers ++ session.cookies.toList)

  def jsonEntity(json: JsValue) = HttpEntity(`application/json`, json.compactPrint)
}


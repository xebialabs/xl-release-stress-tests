package com.xebialabs.xlrelease.stress.dsl.http.libs.http

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model._
import cats._
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.defaults.http.client.{headers => defaultHeaders}
import com.xebialabs.xlrelease.stress.domain.HttpSession
import com.xebialabs.xlrelease.stress.dsl.http.DSL
import com.xebialabs.xlrelease.stress.dsl.http.libs.Api
import com.xebialabs.xlrelease.stress.utils.JsUtils
import com.xebialabs.xlrelease.stress.utils.JsUtils.JsParsed
import freestyle.free._
import freestyle.free.implicits._
import spray.json._

trait Json[F[_]] extends Api[F] { self =>
  import Json._

  private def api: DSL[F] = self._api

  trait GetJson {
    def plain(uri: Uri, headers: List[HttpHeader] = defaultHeaders): Program[JsValue] =
      api.http.get(uri, headers) flatMap api.http.parseJson

    def apply(uri: Uri, headers: List[HttpHeader] = defaultHeaders)
             (implicit session: HttpSession): Program[JsValue] =
      plain(uri, headers ++ session.cookies.toList)
  }


  trait PostJson {
    def plain(uri: Uri, json: JsValue, headers: List[HttpHeader] = defaultHeaders): Program[HttpResponse] = {
      api.http.post(uri, json.toHttpEntity, headers)
    }

    def apply(uri: Uri, json: JsValue, headers: List[HttpHeader] = defaultHeaders)
             (implicit session: HttpSession): Program[HttpResponse] =
      plain(uri, json, headers ++ session.cookies.toList)
  }

  trait PutJson {
    def plain(uri: Uri, json: JsValue, headers: List[HttpHeader] = defaultHeaders): Program[HttpResponse] =
      api.http.put(uri, json.toHttpEntity, headers)

    def apply(uri: Uri, json: JsValue, headers: List[HttpHeader] = defaultHeaders)
             (implicit session: HttpSession): Program[HttpResponse] =
      plain(uri, json, headers ++ session.cookies.toList)
  }

  trait JsonLib {
    val get: GetJson = new GetJson {}
    val put: PutJson = new PutJson {}
    val post: PostJson = new PostJson {}
  }

  val json: JsonLib = new JsonLib {}

}

object Json {
  implicit class JsValueOps(val json: JsValue) extends AnyVal {
    def toHttpEntity: RequestEntity = HttpEntity(`application/json`, json.compactPrint)
  }
}

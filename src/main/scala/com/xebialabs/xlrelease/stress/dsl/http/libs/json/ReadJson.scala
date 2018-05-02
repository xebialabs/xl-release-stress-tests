package com.xebialabs.xlrelease.stress.dsl.http.libs.json

import akka.http.scaladsl.model.HttpResponse
import cats.implicits._
import com.xebialabs.xlrelease.stress.dsl.http.DSL
import com.xebialabs.xlrelease.stress.dsl.http.libs.Api
import com.xebialabs.xlrelease.stress.utils.JsUtils
import com.xebialabs.xlrelease.stress.utils.JsUtils.JsParsed
import spray.json.{DeserializationException, JsValue}
import freestyle.free._
import freestyle.free.implicits._


trait ReadJson[F[_]] extends Api[F] { self =>

  private def api: DSL[F] = self._api

  def read[A](reader: JsValue => JsParsed[A])(value: JsValue): Program[A] =
    reader(value) match {
      case Left(err) =>
        error(err.msg, value, err.fieldNames, err.cause)
      case Right(result) =>
        result.pure[Program]
    }

  def parse[A](reader: JsValue => JsParsed[A])(resp: HttpResponse): Program[A] =
    api.http.parseJson(resp) flatMap read(reader)


  protected def error[A](msg: String, original: JsValue, fieldNames: List[String] = Nil, cause: Throwable = null): Program[A] =
    api.error.error[A](DeserializationException(msg, JsUtils.debug(msg)(original), fieldNames))
}

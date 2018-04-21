package com.xebialabs.xlrelease.stress.http.handlers.future

import com.xebialabs.xlrelease.stress.http.Json
import spray.json.{DeserializationException, JsValue}

import scala.concurrent.Future

object JsonHandler {
  implicit def jsonHandler: Json.Handler[Future] = new Json.Handler[Future] {
    protected def parsed[A](value: A): Future[A] =
      Future.successful(value)

    protected def error[A](msg: String, original: JsValue, fieldNames: List[String] = Nil, cause: Throwable = null): Future[A] = {
      Future.failed(
        DeserializationException(s"Failed to parse JSON: $msg.\nJSON:\n${original.prettyPrint}", cause, fieldNames)
      )
    }
  }
}

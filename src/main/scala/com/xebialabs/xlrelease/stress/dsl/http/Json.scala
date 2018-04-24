package com.xebialabs.xlrelease.stress.dsl.http

import com.xebialabs.xlrelease.stress.utils.JsUtils.JsParsed
import freestyle.free._
import spray.json._

@free trait Json {
  def parsed[A](value: A): FS[A]
  def error[A](msg: String, original: JsValue, fieldNames: List[String] = Nil, cause: Throwable = null): FS[A]

  def read[A](reader: JsValue => JsParsed[A])(value: JsValue): FS[A] =
    reader(value) match {
      case Left(err) =>
        error(err.msg, value, err.fieldNames, err.cause)
      case Right(result) =>
        parsed(result)
    }
}


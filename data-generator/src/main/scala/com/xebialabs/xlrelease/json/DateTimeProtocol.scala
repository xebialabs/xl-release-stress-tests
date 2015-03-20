package com.xebialabs.xlrelease.json

import spray.json._
import spray.json.JsString
import spray.json.JsValue
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTime
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol

private [json] trait DateTimeProtocol extends DefaultJsonProtocol {
  implicit object DateTimeFormat extends RootJsonFormat[DateTime] {

    val formatter = ISODateTimeFormat.dateTimeNoMillis

    def write(obj: DateTime): JsValue = {
      JsString(formatter.print(obj))
    }

    def read(json: JsValue): DateTime = json match {
      case JsString(s) => try {
        formatter.parseDateTime(s)
      }
      catch {
        case t: Throwable => error(s)
      }
      case _ =>
        error(json.toString())
    }

    def error(v: Any): DateTime = {
      val example = formatter.print(0)
      deserializationError(f"'$v' is not a valid date value. Dates must be in compact ISO-8601 format, e.g. '$example'")
    }
  }
}
package com.xebialabs.xlrelease.stress.parsers.dataset

import java.text.SimpleDateFormat
import java.util.Date

import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat, _}

import scala.util.Try

case class CreateReleaseArgs(releaseTitle: String, releaseVariables: Map[String, String], releasePasswordVariables: Map[String, String],
                             scheduledStartDate: Date = new Date(), autoStart: Boolean = false)

object Release {
  type ID = String
}

object CreateReleaseArgs extends DefaultJsonProtocol {
  implicit val createReleaseArgsFormat: RootJsonFormat[CreateReleaseArgs] = jsonFormat5(CreateReleaseArgs.apply)

  implicit object dateFormat extends JsonFormat[Date] {
    def read(json: JsValue) = json match {
      case JsString(rawDate) =>
        parseIsoDateString(rawDate)
          .fold(deserializationError(s"Expected ISO Date format, got $rawDate"))(identity)
      case error => deserializationError(s"Expected JsString, got $error")
    }

    def write(date: Date) = JsString(dateToIsoString(date))
  }

  private val localIsoDateFormatter = new ThreadLocal[SimpleDateFormat] {
    override def initialValue() = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  }

  private def dateToIsoString(date: Date) =
    localIsoDateFormatter.get().format(date)

  private def parseIsoDateString(date: String): Option[Date] =
    Try {
      localIsoDateFormatter.get().parse(date)
    }.toOption
}

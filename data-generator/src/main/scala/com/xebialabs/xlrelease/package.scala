package com.xebialabs

import com.xebialabs.xlrelease.domain._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.{Failure, Success}

package object xlrelease {

  trait DateTimeProtocol extends DefaultJsonProtocol {
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

  trait XlrJsonProtocol extends DefaultJsonProtocol with AdditionalFormats with DateTimeProtocol {
    implicit val releaseFormat = jsonFormat8(Release.apply)
    implicit val phaseFormat = jsonFormat5(Phase.apply)
    implicit val taskFormat = jsonFormat4(Task.apply)

    implicit val userFormat = jsonFormat5(User)
    implicit val roleFormat = jsonFormat2(Role)
    implicit val puserFormat = jsonFormat2(PUser)
    implicit val principalFormat = jsonFormat2(Principal)
    implicit val permissionFormat = jsonFormat2(Permission)
  }

  // Helper functions

  def printedFuture(f: Future[Any]): Future[Any] = f.andThen {
    case Success(t) => println(t)
    case Failure(e) => e.printStackTrace()
  }

  implicit class PrintableFuture(f: Future[Any]) {

    def thenPrint(): Future[Any] = f.andThen {
        case Success(t) => println(t)
        case Failure(e) => e.printStackTrace()
      }
  }

}

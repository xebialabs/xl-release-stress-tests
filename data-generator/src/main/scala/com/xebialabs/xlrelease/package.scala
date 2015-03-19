package com.xebialabs

import com.xebialabs.xlrelease.domain._
import spray.json.{AdditionalFormats, DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.{Failure, Success}

package object xlrelease {

  trait XlrJsonProtocol extends DefaultJsonProtocol with AdditionalFormats {
    implicit val releaseFormat = jsonFormat4(Release.apply)
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

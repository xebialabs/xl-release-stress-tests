package com.xebialabs.xlrelease.stress.client.protocol

import java.util.Date

import com.xebialabs.xlrelease.stress.client.utils.DateFormat
import spray.json._



case class CreateReleaseArgs(title: String,
                             variables: Map[String, String],
                             passwordVariables: Map[String, String] = Map.empty,
                             scheduledStartDate: Date = new Date(),
                             autoStart: Boolean = false)

object CreateReleaseArgs extends DefaultJsonProtocol with DateFormat {
  implicit val createReleaseArgsWriter: RootJsonWriter[CreateReleaseArgs] = cra => JsObject(
    "releaseTitle" -> cra.title.toJson,
    "releaseVariables" -> cra.variables.toJson,
    "releasePasswordVariables" -> cra.passwordVariables.toJson,
    "scheduledStartDate" -> cra.scheduledStartDate.toJson
  )
}
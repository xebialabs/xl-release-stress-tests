package com.xebialabs.xlrelease.stress

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.headers.Accept
import freestyle.free._
import freestyle.free.effects.error.ErrorM
import freestyle.free.logging.LoggingM

package object http {
  val defaultHeaders: List[HttpHeader] = List(Accept(`application/json`))

  @module trait Http {
    val client: Client
    val json: Json
    val time: Time

    val log: LoggingM
    val error: ErrorM
  }

  type API = Http[Http.Op]
  type Program[A] = FreeS[Http.Op, A]

  type HttpProgram[A] = FreeS[Client.Op, A]
}

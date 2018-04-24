package com.xebialabs.xlrelease.stress.dsl

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.headers.Accept
import freestyle.free.effects.error.ErrorM
import freestyle.free.logging.LoggingM
import freestyle.free.{FreeS, module}

package object http {

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

  val defaultHeaders: List[HttpHeader] = List(Accept(`application/json`))
}

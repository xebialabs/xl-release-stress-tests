package com.xebialabs.xlrelease.stress.config

import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.headers.Accept

object defaults {
  object http {
    object client {
      val headers = List(Accept(`application/json`))
    }
  }
}

package com.xebialabs.xlrelease.stress.client.utils

import com.github.nscala_time.time.Imports._

import spray.json._

trait DateFormat extends DefaultJsonProtocol {
  implicit val dateWriter: RootJsonWriter[DateTime] = date => date.toString().toJson
}

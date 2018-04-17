package com.xebialabs.xlrelease.stress.client.utils

import java.text.SimpleDateFormat
import java.util.Date

import spray.json._

trait DateFormat extends DefaultJsonProtocol {
  implicit val dateWriter: RootJsonWriter[Date] = date => dateToIsoString(date).toJson

  private val localIsoDateFormatter = new ThreadLocal[SimpleDateFormat] {
    override def initialValue() = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  }

  private def dateToIsoString(date: Date): String =
    localIsoDateFormatter.get().format(date)

}

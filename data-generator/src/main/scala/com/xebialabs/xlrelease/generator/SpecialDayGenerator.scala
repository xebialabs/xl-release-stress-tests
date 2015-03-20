package com.xebialabs.xlrelease.generator

import com.xebialabs.xlrelease.domain.{SpecialDay, Directory, Ci}

object SpecialDayGenerator {

  def generateSpecialDays(): Seq[Ci] = {
    val directory = Directory("Configuration/Calendar")
    val days = Seq("20141228", "20141229", "20150107", "20150112", "20150130", "20150207")
    directory +: days.map(day => SpecialDay(directory.id + "/" + day, day, day))
  }
}

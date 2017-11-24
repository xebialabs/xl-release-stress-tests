package com.xebialabs.xlrelease.generator

import com.xebialabs.xlrelease.domain.{Ci, SpecialDay}

object SpecialDayGenerator {

  def generateSpecialDays(): Seq[Ci] = {
    val days = Seq("20141228", "20141229", "20150107", "20150112", "20150130", "20150207")
    days.map(day => SpecialDay("Configuration/Calendar/" + day, day))
  }
}

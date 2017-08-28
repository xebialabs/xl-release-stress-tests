package com.xebialabs.xlrelease.generator

import com.xebialabs.xlrelease.domain.{Directory, SpecialDay}
import com.xebialabs.xlrelease.generator.SpecialDayGenerator.generateSpecialDays
import com.xebialabs.xlrelease.support.UnitTestSugar
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class SpecialDayGeneratorTest extends UnitTestSugar {

  describe("Special day generator") {

    it("should not generate a directory") {
      val directories = generateSpecialDays().filter(_.isInstanceOf[Directory])

      directories shouldBe 'empty
    }

    it("should generate some special days") {
      val specialDays = generateSpecialDays().filter(_.isInstanceOf[SpecialDay])

      specialDays.size should be > 1
      specialDays.head.id should startWith("Configuration/Calendar/")
    }
  }
}

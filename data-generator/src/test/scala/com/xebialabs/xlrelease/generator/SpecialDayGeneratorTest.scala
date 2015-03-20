package com.xebialabs.xlrelease.generator

import com.xebialabs.xlrelease.domain.{SpecialDay, Ci, Directory}
import com.xebialabs.xlrelease.generator.SpecialDayGenerator.generateSpecialDays
import com.xebialabs.xlrelease.support.UnitTestSugar
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class SpecialDayGeneratorTest extends UnitTestSugar {

  describe("Special day generator") {

    it("should generate a directory") {
      val directories = generateSpecialDays().filter(_.isInstanceOf[Directory])

      directories should have size 1
      directories.head.id shouldBe "Configuration/Calendar"
    }

    it("should generate some special days") {
      val specialDays = generateSpecialDays().filter(_.isInstanceOf[SpecialDay])

      specialDays.size should be > 1
      specialDays.head.id should startWith("Configuration/Calendar/")
    }
  }
}

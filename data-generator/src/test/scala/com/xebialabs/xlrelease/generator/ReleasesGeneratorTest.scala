package com.xebialabs.xlrelease.generator

import com.xebialabs.xlrelease.domain.{Ci, Phase, Release, Task}
import com.xebialabs.xlrelease.generator.ReleasesGenerator._
import com.xebialabs.xlrelease.support.UnitTestSugar


class ReleasesGeneratorTest extends UnitTestSugar {


  describe("release generator") {

    it("should return empty seq if called with 0") {
      ReleasesGenerator.generateCompletedReleases(0).flatten shouldBe 'empty
    }

    it("should generate completed releases with default amount of phases and tasks") {
      val amount = 5
      val cis = ReleasesGenerator.generateCompletedReleases(amount).flatten
      cis.filter(_.isInstanceOf[Release]) should have size amount
      cis.filter(_.isInstanceOf[Phase]) should have size amount * phasesPerRelease
      cis.filter(_.isInstanceOf[Task]) should have size amount * phasesPerRelease * tasksPerPhase
    }

    it("should slice CIs according to releases") {
      val batches = ReleasesGenerator.generateCompletedReleases(5)

      batches should have size 5

      batches.foreach( batch => {
        batch.filter(_.isInstanceOf[Release]) should have size 1

        val releaseOfTheBatch = batch.find(_.isInstanceOf[Release]).get

        batch.filterNot(_.isInstanceOf[Release]).foreach( ci => {
          ci.id should startWith(releaseOfTheBatch.id)
        })
      })
    }


  }
}

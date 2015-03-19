package com.xebialabs.xlrelease.generator

import com.xebialabs.xlrelease.domain.{Ci, Phase, Release, Task}
import com.xebialabs.xlrelease.generator.ReleasesGenerator._
import com.xebialabs.xlrelease.support.UnitTestSugar
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
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

        val release = releaseOfBatch(batch)

        phasesAndTasksOfBatch(batch).foreach( ci => {
          ci.id should startWith(release.id)
        })
      })
    }

    it("should generate completed releases") {
      val cis = ReleasesGenerator.generateCompletedReleases(1).head

      val release = releaseOfBatch(cis)
      release.status should be("COMPLETED")
      release.queryableEndDate.isAfter(release.queryableStartDate) should be(right = true)
      release.dueDate.isAfter(release.scheduledStartDate) should be(right = true)

      phasesAndTasksOfBatch(cis).foreach( ci => {
        ci.status should be("COMPLETED")
      })
    }

    it("should generate template releases") {
      val cis = ReleasesGenerator.generateTemplateReleases(1).head

      val release = releaseOfBatch(cis)
      release.status should be("TEMPLATE")

      phasesAndTasksOfBatch(cis).foreach( ci => {
        ci.status should be("PLANNED")
      })
    }
  }

  def phasesAndTasksOfBatch(batch: Seq[Ci]): Seq[Ci] = {
    batch.filterNot(_.isInstanceOf[Release])
  }

  def releaseOfBatch(batch: Seq[Ci]): Release = {
    batch.find(_.isInstanceOf[Release]).get.asInstanceOf[Release]
  }
}

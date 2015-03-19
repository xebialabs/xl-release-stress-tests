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
      releasesOfBatch(cis) should have size amount
      phasesOfBatch(cis) should have size amount * phasesPerRelease
      tasksOfBatch(cis) should have size amount * phasesPerRelease * tasksPerPhase
    }

    it("should slice CIs according to releases") {
      val batches = ReleasesGenerator.generateCompletedReleases(5)

      batches should have size 5

      batches.foreach( batch => {
        releasesOfBatch(batch) should have size 1

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

    it("should generate active releases") {
      val cis = ReleasesGenerator.generateActiveReleases(1).head

      val release = releaseOfBatch(cis)
      release.status should be("IN_PROGRESS")

      val activePhases = phasesOfBatch(cis).filter(_.status == "IN_PROGRESS")
      activePhases should have size 1

      val activeTasks = tasksOfBatch(cis).filter(_.status == "IN_PROGRESS")
      activeTasks should have size 1
    }

    it("should generate the dependent release") {
      val cis = ReleasesGenerator.generateDependentRelease()

      val release = releaseOfBatch(cis)
      release.id should be(ReleasesGenerator.dependentReleaseId)
      release.status should be("PLANNED")

      phasesAndTasksOfBatch(cis).foreach( ci => {
        ci.status should be("PLANNED")
      })
    }
  }

  def releasesOfBatch(cis: Seq[Ci]): Seq[Release] = {
    cis.filter(_.isInstanceOf[Release]).asInstanceOf[Seq[Release]]
  }

  def releaseOfBatch(batch: Seq[Ci]): Release = {
    batch.find(_.isInstanceOf[Release]).get.asInstanceOf[Release]
  }

  def phasesOfBatch(cis: Seq[Ci]): Seq[Phase] = {
    cis.filter(_.isInstanceOf[Phase]).asInstanceOf[Seq[Phase]]
  }

  def tasksOfBatch(cis: Seq[Ci]): Seq[Task] = {
    cis.filter(_.isInstanceOf[Task]).asInstanceOf[Seq[Task]]
  }

  def phasesAndTasksOfBatch(batch: Seq[Ci]): Seq[Ci] = {
    batch.filterNot(_.isInstanceOf[Release])
  }
}

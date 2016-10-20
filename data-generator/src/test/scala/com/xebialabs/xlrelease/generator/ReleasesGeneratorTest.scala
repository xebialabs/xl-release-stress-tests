package com.xebialabs.xlrelease.generator

import com.typesafe.config.ConfigFactory
import com.xebialabs.xlrelease.domain._
import com.xebialabs.xlrelease.generator.ReleaseGenerator._
import com.xebialabs.xlrelease.support.UnitTestSugar
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ReleasesGeneratorTest extends UnitTestSugar {

  var generator: ReleasesGenerator = _

  implicit val config = ConfigFactory.load()

  override protected def beforeEach(): Unit = {
    generator = new ReleasesGenerator()
  }

  describe("release generator") {

    it("should return empty seq if called with 0") {
      generator.generateCompletedReleases(0)._1.flatten shouldBe 'empty
    }

    it("should generate completed releases with default amount of phases and tasks") {
      val amount = 5
      val cis = generator.generateCompletedReleases(amount)._1.flatten
      releasesOfBatch(cis) should have size amount
      phasesOfBatch(cis) should have size amount * phasesPerRelease
      tasksOfBatch(cis) should have size amount * phasesPerRelease * tasksPerPhase
    }

    it("should slice CIs according to releases") {
      val (batches, _) = generator.generateCompletedReleases(5)

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
      val cis = generator.generateCompletedReleases(1)._1.head

      val release = releaseOfBatch(cis)
      release.status should be("COMPLETED")
      release.queryableEndDate.isAfter(release.queryableStartDate) should be(right = true)
      release.endDate.get.isAfter(release.queryableStartDate) should be(right = true)
      release.dueDate.isAfter(release.scheduledStartDate) should be(right = true)

      phasesAndTasksOfBatch(cis).foreach( ci => {
        ci.status should be("COMPLETED")
      })
    }

    it("should generate template releases") {
      val cis = generator.generateTemplateReleases(1).head

      val release = releaseOfBatch(cis)
      release.status should be("TEMPLATE")

      phasesAndTasksOfBatch(cis).foreach( ci => {
        ci.status should be("PLANNED")
      })
    }

    it("should generate active releases") {
      val cis = generator.generateActiveReleases(1).head

      val release = releaseOfBatch(cis)
      release.status should be("IN_PROGRESS")
      release.endDate shouldBe None

      val activePhases = phasesOfBatch(cis).filter(_.status == "IN_PROGRESS")
      activePhases should have size 1

      val activeTasks = tasksOfBatch(cis).filter(_.status == "IN_PROGRESS")
      activeTasks should have size 1
    }

    it("should generate the dependent release") {
      val cis = generator.generateDependentRelease()

      val release = releaseOfBatch(cis)
      release.id should be(dependentReleaseId)
      release.status should be("PLANNED")

      phasesAndTasksOfBatch(cis).foreach( ci => {
        ci.status should be("PLANNED")
      })
    }

    it("should add one gate on each release with a dependency on the dependent release") {
      val cis = generator.generateActiveReleases(1).head

      val gates = tasksOfBatch(cis).filter(_.`type` == "xlrelease.GateTask")
      gates should have size 1

      val dependencies = dependenciesOfBatch(cis)
      dependencies should have size 1
      dependencies.head.id should startWith(gates.head.id)
      dependencies.head.target should be(dependentReleaseId)
    }

    it("should add one attachment to each release and one attachment to first task of each phase") {
      val cis = generator.generateActiveReleases(1).head

      val attachments = cis.filter(_.`type` == "xlrelease.Attachment")

      attachments.head.asInstanceOf[Attachment].fileUri shouldBe "http://localhost:5516/static/0/js/xlrelease.js"

      val attachmentNumbers = attachments.map(_.id.replaceAll(".*Attachment", ""))
      attachmentNumbers shouldEqual (1 to 6).map(_.toString)
      val task21 = cis.find(_.id.contains("/Phase2/Task1")).get.asInstanceOf[Task]
      task21.attachments should have size 1
      task21.attachments.head should fullyMatch regex "Applications/Release_\\d+_1/Attachment2"
    }

    it("should generate 2 folder levels and 10 folders") {
      val folders: Seq[Ci] = generator.generateFolders(10, 2)
      //TODO ...
      folders should have size 110
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

  def dependenciesOfBatch(cis: Seq[Ci]): Seq[Dependency] = {
    cis.filter(_.isInstanceOf[Dependency]).asInstanceOf[Seq[Dependency]]
  }

  def phasesAndTasksOfBatch(batch: Seq[Ci]): Seq[PlanItem] = {
    batch.filter(x => x.isInstanceOf[Phase] || x.isInstanceOf[Task]).asInstanceOf[Seq[PlanItem]]
  }
}

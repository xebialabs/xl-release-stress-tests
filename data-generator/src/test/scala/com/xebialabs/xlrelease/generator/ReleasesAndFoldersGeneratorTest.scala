package com.xebialabs.xlrelease.generator

import com.typesafe.config.{Config, ConfigFactory}
import com.xebialabs.xlrelease.domain._
import com.xebialabs.xlrelease.generator.ReleasesAndFoldersGenerator._
import com.xebialabs.xlrelease.support.UnitTestSugar
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ReleasesAndFoldersGeneratorTest extends UnitTestSugar {

  var generator: ReleasesAndFoldersGenerator = _

  override protected def beforeEach(): Unit = {
    implicit val config: Config = ConfigFactory.load()
    generator = new ReleasesAndFoldersGenerator()
  }

  describe("generator of releases and templates") {

    it("should return empty seq if called with 0") {
      generator.generateCompletedReleases(0) shouldBe 'empty
    }

    it("should generate completed releases with default amount of phases and tasks") {
      val amount = 5
      val releases = generator.generateCompletedReleases(amount)
      releases should have size amount
      phasesOfBatch(releases) should have size amount * phasesPerRelease
      tasksOfBatch(releases) should have size amount * phasesPerRelease * tasksPerPhase
    }

    it("should slice CIs according to releases") {
      val releases = generator.generateCompletedReleases(5)

      releases.foreach(release => {
        phasesAndTasksOfBatch(Seq(release)).foreach(ci => {
          ci.id should startWith(release.release.id)
        })
      })
    }

    it("should generate completed releases") {
      val releases = generator.generateCompletedReleases(1)

      val release = releaseOfBatch(releases)
      release.status should be("COMPLETED")
      release.queryableEndDate.isAfter(release.queryableStartDate) shouldBe true
      release.endDate.get.isAfter(release.queryableStartDate) shouldBe true
      release.dueDate.isAfter(release.scheduledStartDate) shouldBe true

      phasesAndTasksOfBatch(releases).foreach(ci => {
        ci.status should be("COMPLETED")
      })
    }

    it("should generate template releases") {
      val templates = generator.generateTemplateReleases(1)

      val release = releaseOfBatch(templates)
      release.status should be("TEMPLATE")

      phasesAndTasksOfBatch(templates).foreach(ci => {
        ci.status should be("PLANNED")
      })
    }

    it("should generate active releases") {
      val releases = generator.generateActiveReleases(1)

      val release = releaseOfBatch(releases)
      release.status should be("IN_PROGRESS")
      release.endDate shouldBe None

      val activePhases = phasesOfBatch(releases).filter(_.status == "IN_PROGRESS")
      activePhases should have size 1

      val activeTasks = tasksOfBatch(releases).filter(_.status == "IN_PROGRESS")
      activeTasks should have size 1
    }

    it("should generate the dependent release") {
      val release = generator.generateDependentRelease()

      release.id should be(dependentReleaseId)
      release.status should be("PLANNED")

      release.phases.flatMap(phase => phase +: phase.tasks).foreach(ci => {
        ci.status should be("PLANNED")
      })
    }

    it("should add one gate on each release with a dependency on the dependent release") {
      val releases = generator.generateActiveReleases(1)

      val gates = tasksOfBatch(releases).filter(_.`type` == "xlrelease.GateTask")
      gates should have size 1

      val dependencies = dependenciesOfBatch(releases)
      dependencies should have size 1
      dependencies.head.id should startWith(gates.head.id)
      dependencies.head.target should be(dependentReleaseId)
    }

    it("should add one attachment to each release and one attachment to first task of each phase") {
      val releases = generator.generateActiveReleases(1)

      val attachments = releases.flatMap(_.release.attachments)

      attachments(1).id should include("2")
      attachments(5).id should include("6")
      val task21 = tasksOfBatch(releases).find(_.id.contains("/Phase2/Task1")).get.asInstanceOf[Task]
      task21.attachments should have size 1
      task21.attachments.head should fullyMatch regex "Applications/Release_\\d+_1/Attachment2_500KB"
    }

    it("should generated automated templates") {
      val releases = generator.generateAutomatedTemplates(1)

      val template = releaseOfBatch(releases)
      template.status should be("TEMPLATE")
      template.allowConcurrentReleasesFromTrigger should be(false)

      phasesOfBatch(releases).foreach { phase =>
        phase.status should be("PLANNED")
      }

      tasksOfBatch(releases).foreach { task =>
        task.`type` should (be("xlrelease.ScriptTask") or be("xlrelease.GateTask"))
        task.status should be("PLANNED")
      }

      val releaseTriggers = releaseTriggersOfBatch(releases)
      releaseTriggers should have size 1

      val releaseTrigger = releaseTriggers.head
      releaseTrigger.id should startWith(template.id)
      releaseTrigger.enabled should be(true)
      releaseTrigger.initialFire should be(false)
      releaseTrigger.pollType should be("REPEAT")
      releaseTrigger.periodicity should be("300")
    }

  }

  describe("generator of folders") {

    it("should generate n folder levels and m folders") {
      val foldersAndRelatedCis: Seq[Ci] = generator.generateFolders(2, 2)._1

      val folders = foldersAndRelatedCis.filter(f => f.id.matches("Applications(/Folder[_\\d]+)+"))
      folders.map(_.id) shouldBe Seq(
        "Applications/Folder_1",
        "Applications/Folder_1/Folder_1_1",
        "Applications/Folder_1/Folder_1_2",
        "Applications/Folder_2",
        "Applications/Folder_2/Folder_2_1",
        "Applications/Folder_2/Folder_2_2"
      )
    }

    it("should generate three default teams and a viewers team in each top level folder") {
      val foldersAndRelatedCis: Seq[Ci] = generator.generateFolders(2, 2)._2

      val teams = foldersAndRelatedCis.filter(f => f.id.contains("/Team"))

      val folder1viewers = teams.find(_.id == "Applications/Folder_1/TeamViewers_1").get.asInstanceOf[Team]
      folder1viewers.members shouldBe Seq("viewer")
      folder1viewers.permissions shouldBe Seq("folder#view", "release#view", "template#view")
      teams.filter(_.id.startsWith("Applications/Folder_1/")).map(_.asInstanceOf[Team].teamName) should contain theSameElementsAs
        Set("Release Admin", "Template Owner", "Folder Owner", "Viewers")
    }

    it("should spread releases across folders") {
      val (Seq(folder1, folder2), _) = generator.generateFolders(amount = 2, levels = 1)
      val Seq(release1, release2, release3) = generator.generateActiveReleases(3)

      release1.release.id should startWith(folder1.id)
      release2.release.id should startWith(folder2.id)
      release3.release.id should startWith(folder1.id)
    }

  }

  describe("generator of dependency trees") {
    it("should generate tree with specified depth and breadth") {
      val depth = 4
      val breadth = 3

      val cis = generator.generateDependencyTrees(1, depth, breadth, breadth)

      val releases = releasesOfBatch(cis)
      releases should have size depth * breadth + 1

      releases.filter(_.title.contains("depth: 1")) should have size breadth
      releases.filter(_.title.contains("number: 2")) should have size depth
    }

    it("should generate dependencies between releases in the tree") {
      val cis = generator.generateDependencyTrees(1, 2, 2, 2)

      val dependencies = dependenciesOfBatch(cis)
      dependencies should have size 6

      val depth0Targets = dependencies.filter(_.id.matches("Applications/Release_\\d+_5.*")).map(_.target)
      val depth1Targets = dependencies.filter(_.id.matches("Applications/Release_\\d+_[34].*")).map(_.target)

      depth0Targets should have size 2
      depth0Targets should contain only(
        s"Applications/Release_${generator.transaction}_3/Phase5/Task10",
        s"Applications/Release_${generator.transaction}_4/Phase5/Task10")

      depth1Targets should have size 4
      depth1Targets.distinct should contain only(
        s"Applications/Release_${generator.transaction}_1/Phase5/Task10",
        s"Applications/Release_${generator.transaction}_2/Phase5/Task10")
    }
  }

  private def releasesOfBatch(releases: Seq[ReleaseAndRelatedCis]): Seq[Release] = {
    releases.map(_.release)
  }

  private def releaseOfBatch(releases: Seq[ReleaseAndRelatedCis]): Release = {
    releasesOfBatch(releases).head
  }

  private def phasesOfBatch(releases: Seq[ReleaseAndRelatedCis]): Seq[Phase] = {
    releases.flatMap(_.release.phases)
  }

  private def tasksOfBatch(releases: Seq[ReleaseAndRelatedCis]): Seq[AbstractTask] = {
    phasesOfBatch(releases).flatMap(_.tasks)
  }

  private def dependenciesOfBatch(releases: Seq[ReleaseAndRelatedCis]): Seq[Dependency] = {
    tasksOfBatch(releases)
      .filter(_.isInstanceOf[GateTask])
      .flatMap(_.asInstanceOf[GateTask].dependencies)
  }

  private def phasesAndTasksOfBatch(releases: Seq[ReleaseAndRelatedCis]): Seq[PlanItem] = {
    phasesOfBatch(releases) ++ tasksOfBatch(releases)
  }

  private def releaseTriggersOfBatch(releases: Seq[ReleaseAndRelatedCis]): Seq[ReleaseTrigger] = {
    releases.flatMap(_.release.releaseTriggers)
  }
}

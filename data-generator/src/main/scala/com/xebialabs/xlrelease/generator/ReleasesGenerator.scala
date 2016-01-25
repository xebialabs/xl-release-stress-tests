package com.xebialabs.xlrelease.generator

import com.xebialabs.xlrelease.domain._
import ReleaseGenerator._

import scala.util.Random

object ReleaseGenerator {
  val phasesPerRelease = 5
  val tasksPerPhase = 10
  val dependentReleaseId = "Applications/ReleaseDependent"
}

class ReleasesGenerator {
  
  var idCounter = 0
  val transaction = Math.abs(Random.nextInt())

  private def incrementCounterAndGet(): Int = {
    idCounter += 1
    idCounter
  }

  def generateCompletedReleases(amount: Int, genComments: Boolean = false): (Seq[Seq[Ci]], Seq[String]) = {
    generateReleases(amount, "COMPLETED", (n) => s"Stress test completed release $n", genComments)
  }

  def generateTemplateReleases(amount: Int, genComments: Boolean = false): Seq[Seq[Ci]] = {
    generateReleases(amount, "TEMPLATE", (n) => s"Stress test template release $n", genComments)._1
  }

  def generateActiveReleases(amount: Int, genComments: Boolean = false): Seq[Seq[Ci]] = {
    generateReleases(amount, "IN_PROGRESS", (n) => s"Stress test active release $n", genComments)._1
  }

  def generateDependentRelease(): Seq[Ci] = {
    val release = Release.build(dependentReleaseId, "Stress test Dependent release", "PLANNED", 1, 1)
    createReleaseContent(release, generateComments = false) :+ release
  }

  def generateDepRelease(relIds: Seq[String], numberOfRel: Int): Seq[Seq[Ci]] = {
    (1 to numberOfRel).zip(relIds).map { case (n, relId) =>
      val releaseNumber = incrementCounterAndGet()
      val release = Release.build(s"Applications/Release_${transaction}_$releaseNumber",
        s"Dependent release #$releaseNumber", "IN_PROGRESS", releaseNumber, numberOfRel)
      createDepRelContent(release, relId) :+ release
    }
  }

  def createDepRelContent(r: Release, depRelId: String): Seq[Ci] = {
    val status: Boolean => String = b => if (b) "IN_PROGRESS" else "PLANNED"
    (1 to 10).flatMap { i =>
      val phase = Phase.build(s"Phase$i", r.id, status(i == 1))
      (1 to 10).flatMap { j =>
          val task = Task.buildGate(s"Task$j", phase.id, status(j == 1 && i == 1))
          Seq(task, Dependency.build("Dependency", task.id, depRelId))
      } :+ phase
    }
  }

  def generateReleases(amount: Int, status: String, titleGenerator: (Int) => String,
                       genComments: Boolean): (Seq[Seq[Ci]], Seq[String]) = {
    val releases = (1 to amount).map { n =>
      Release.build(s"Applications/Release_${transaction}_${incrementCounterAndGet()}",titleGenerator(n), status, n, amount)
    }
    releases.map(release => createReleaseContent(release, genComments) :+ release) -> releases.map(_.id)
  }

  private def createReleaseContent(release: Release, generateComments: Boolean): Seq[Ci] = {
    val phaseNumbers = 1 to phasesPerRelease
    val phases: Seq[Phase] = phaseNumbers.map(n =>
      Phase.build(s"Phase$n", release.id, phaseStatus(release, n)))

    val cis: Seq[Ci] = phases.zip(phaseNumbers).flatMap {
      case (phase, phaseNumber) =>
        (1 to tasksPerPhase).flatMap { taskNumber =>
          val taskCis = makeTaskCis(phase, phaseNumber, taskNumber)
          taskCis ++ (if (generateComments) makeCommentCis(taskCis.filter(_.`type` != "xlrelease.Dependency")) else Nil)
        }
    }

    phases ++ cis
  }

  private def makeCommentCis(parentIds: Seq[Ci]): Seq[Ci] =
    parentIds.zipWithIndex.map { case (p, n) => Comment.buildComment(s"Comment$n", p.id) }

  private def makeTaskCis(phase: Phase, phaseNumber: Int, taskNumber: Int): Seq[Ci] = {
    val task = Task.build(s"Task$taskNumber", phase.id, taskStatus(phase, taskNumber))

    if (isLastTaskOfRelease(phaseNumber, taskNumber)) {
      val dependency = Dependency.build("Dependency", task.id, dependentReleaseId)
      Seq(task.toGate, dependency)
    }
    else
      Seq(task)
  }

  private def isLastTaskOfRelease(phaseNumber: Int, taskNumber: Int): Boolean = {
    phaseNumber == phasesPerRelease && taskNumber == tasksPerPhase
  }

  private def phaseStatus(release: Release, phaseNumber: Int): String = {
    release.status match {
      case "TEMPLATE" => "PLANNED"
      case "IN_PROGRESS" => if (phaseNumber == 1)  "IN_PROGRESS" else "PLANNED"
      case _ => release.status
    }
  }

  private def taskStatus(phase: Phase, taskNumber: Int): String = {
    phase.status match {
      case "IN_PROGRESS" => if (taskNumber == 1)  "IN_PROGRESS" else "PLANNED"
      case _ => phase.status
    }
  }
}

package com.xebialabs.xlrelease.generator

import com.xebialabs.xlrelease.domain._
import ReleaseGenerator._

object ReleaseGenerator {
  val phasesPerRelease = 5
  val tasksPerPhase = 10
  val dependentReleaseId = "Applications/ReleaseDependent"
}

class ReleasesGenerator {
  
  var idCounter = 0

  private def incrementCounterAndGet(): Int = {
    idCounter += 1
    idCounter
  }

  def generateCompletedReleases(amount: Int): Seq[Seq[Ci]] = {
    generateReleases(amount, "COMPLETED", (n) => s"Stress test completed release $n")
  }

  def generateTemplateReleases(amount: Int): Seq[Seq[Ci]] = {
    generateReleases(amount, "TEMPLATE", (n) => s"Stress test template release $n")
  }

  def generateActiveReleases(amount: Int): Seq[Seq[Ci]] = {
    generateReleases(amount, "IN_PROGRESS", (n) => s"Stress test active release $n")
  }

  def generateDependentRelease(): Seq[Ci] = {
    val release = Release.build(dependentReleaseId, "Stress test Dependent release", "PLANNED", 1, 1)
    createReleaseContent(release) :+ release
  }

  def generateReleases(amount: Int, status: String, titleGenerator: (Int) => String): Seq[Seq[Ci]] = {
    val releases = (1 to amount).map(n => {
      val releaseNumber = incrementCounterAndGet()
      Release.build(s"Applications/Release$releaseNumber", titleGenerator(n), status, n, amount)
    })

    releases.map(release => createReleaseContent(release) :+ release)
  }

  private def createReleaseContent(release: Release): Seq[Ci] = {
    val phaseNumbers = 1 to phasesPerRelease
    val phases: Seq[Phase] = phaseNumbers.map(n =>
      Phase.build(s"Phase$n", release.id, phaseStatus(release, n)))

    val cis: Seq[Ci] = phases.zip(phaseNumbers).flatMap {
      case (phase, phaseNumber) =>
        (1 to tasksPerPhase).flatMap(taskNumber =>
          makeTaskCis(phase, phaseNumber, taskNumber))
    }

    phases ++ cis
  }

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

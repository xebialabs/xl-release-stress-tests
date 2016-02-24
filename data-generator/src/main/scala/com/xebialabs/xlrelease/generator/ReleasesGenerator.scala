package com.xebialabs.xlrelease.generator

import com.typesafe.config.Config
import com.xebialabs.xlrelease.domain._
import com.xebialabs.xlrelease.generator.ReleaseGenerator._

import scala.util.Random

object ReleaseGenerator {
  val phasesPerRelease = 5
  val tasksPerPhase = 10
  val dependentReleaseId = "Applications/ReleaseDependent"
}

class ReleasesGenerator {
  
  var releaseIdCounter = 0
  var attachmentIdCounter = 0
  val transaction = Math.abs(Random.nextInt())

  private def incrementReleaseIdCounterAndGet(): Int = {
    releaseIdCounter += 1
    releaseIdCounter
  }

  private def incrementAttachmentIdCounterAndGet(): Int = {
    attachmentIdCounter += 1
    attachmentIdCounter
  }

  def generateCompletedReleases(amount: Int)(implicit config: Config): Seq[Seq[Ci]] = {
    generateReleases(amount, "COMPLETED", (n) => s"Stress test completed release $n")
  }

  def generateTemplateReleases(amount: Int)(implicit config: Config): Seq[Seq[Ci]] = {
    generateReleases(amount, "TEMPLATE", (n) => s"Stress test template release $n")
  }

  def generateActiveReleases(amount: Int)(implicit config: Config): Seq[Seq[Ci]] = {
    generateReleases(amount, "IN_PROGRESS", (n) => s"Stress test active release $n")
  }

  def generateDependentRelease()(implicit config: Config): Seq[Ci] = {
    val release = Release.build(dependentReleaseId, "Stress test Dependent release", "PLANNED", 1, 1)
    createReleaseContent(release) :+ release
  }

  def generateReleases(amount: Int, status: String, titleGenerator: (Int) => String)(implicit config: Config): Seq[Seq[Ci]] = {
    val releases = (1 to amount).map(n => {
      val releaseNumber = incrementReleaseIdCounterAndGet()
      Release.build(s"Applications/Release_${transaction}_$releaseNumber", titleGenerator(n), status, n, amount)
    })

    releases.map(release => createReleaseContent(release) :+ release)
  }

  def generateAttachments(amount: Int, containerId: String)(implicit config: Config): Seq[Ci] = {
    for (_ <- 1 to amount) yield Attachment.build(s"Attachment${incrementAttachmentIdCounterAndGet()}", containerId)
  }

  private def createReleaseContent(release: Release)(implicit config: Config): Seq[Ci] = {
    val phaseNumbers = 1 to phasesPerRelease
    val phases: Seq[Phase] = phaseNumbers.map(n =>
      Phase.build(s"Phase$n", release.id, phaseStatus(release, n)))

    val taskCis: Seq[Ci] = phases.zip(phaseNumbers).flatMap {
      case (phase, phaseNumber) =>
        (1 to tasksPerPhase).flatMap(taskNumber =>
          makeTaskCis(phase, phaseNumber, taskNumber, release.id))
    }

    val releaseAttachments: Seq[Ci] = generateAttachments(1, release.id)

    phases ++ taskCis ++ releaseAttachments
  }

  private def makeTaskCis(phase: Phase, phaseNumber: Int, taskNumber: Int, releaseId: String)(implicit config: Config): Seq[Ci] = {
    if (isFirstTaskOfPhase(taskNumber)) {
      val attachment = generateAttachments(1, releaseId).head
      val task = Task.build(s"Task$taskNumber", phase.id, taskStatus(phase, taskNumber), attachments = List(attachment.id))
      Seq(task, attachment)
    } else if (isLastTaskOfRelease(phaseNumber, taskNumber)) {
      val task = Task.buildGate(s"Task$taskNumber", phase.id, taskStatus(phase, taskNumber))
      val dependency = Dependency.build("Dependency", task.id, dependentReleaseId)
      Seq(task, dependency)
    } else {
      val task = Task.build(s"Task$taskNumber", phase.id, taskStatus(phase, taskNumber))
      Seq(task)
    }
  }

  private def isFirstTaskOfPhase(taskNumber: Int): Boolean = taskNumber == 1

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

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

  def generateCompletedReleases(amount: Int, genComments: Boolean = false)(implicit config: Config): (Seq[Seq[Ci]], Seq[String]) = {
    generateReleases(amount, "COMPLETED", (n) => s"Stress test completed release $n", genComments)
  }

  def generateTemplateReleases(amount: Int, genComments: Boolean = false)(implicit config: Config): Seq[Seq[Ci]] = {
    generateReleases(amount, "TEMPLATE", (n) => s"Stress test template release $n", genComments)._1
  }

  def generateActiveReleases(amount: Int, genComments: Boolean = false)(implicit config: Config): Seq[Seq[Ci]] = {
    generateReleases(amount, "IN_PROGRESS", (n) => s"Stress test active release $n", genComments)._1
  }

  def generateDependentRelease()(implicit config: Config): Seq[Ci] = {
    val release = Release.build(dependentReleaseId, "Stress test Dependent release", "PLANNED", 1, 1)
    createReleaseContent(release, generateComments = false) :+ release
  }

  def generateDepRelease(relIds: Seq[String], numberOfRel: Int): Seq[Seq[Ci]] = {
    (1 to numberOfRel).zip(relIds).map { case (n, relId) =>
      val releaseNumber = incrementReleaseIdCounterAndGet()
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
                       genComments: Boolean)(implicit config: Config): (Seq[Seq[Ci]], Seq[String]) = {
    val releases = (1 to amount).map { n =>
      val releaseNumber = incrementReleaseIdCounterAndGet()
      Release.build(s"Applications/Release_${transaction}_$releaseNumber", titleGenerator(n), status, n, amount)
    }

    releases.map(release => createReleaseContent(release, genComments) :+ release) -> releases.map(_.id)
  }

  def generateAttachments(amount: Int, containerId: String)(implicit config: Config): Seq[Ci] = {
    for (_ <- 1 to amount) yield Attachment.build(s"Attachment${incrementAttachmentIdCounterAndGet()}", containerId)
  }

  def generateActivityLogs(amount: Int, releaseId: String) : Seq[Ci] = {
    val directory = ActivityLogDirectory.build(releaseId)
    val entries = for (i <- 1 to amount) yield ActivityLogEntry.build(directory.id, message = s"Did some activity $i")
    List(directory) ++ entries
  }

  private def createReleaseContent(release: Release, generateComments: Boolean)(implicit config: Config): Seq[Ci] = {
    val phaseNumbers = 1 to phasesPerRelease
    val phases: Seq[Phase] = phaseNumbers.map(n =>
      Phase.build(s"Phase$n", release.id, phaseStatus(release, n)))

    val cis: Seq[Ci] = phases.zip(phaseNumbers).flatMap {
      case (phase, phaseNumber) =>
        (1 to tasksPerPhase).flatMap { taskNumber =>
          val taskCis = makeTaskCis(phase, phaseNumber, taskNumber, release.id)
          taskCis ++ (if (generateComments) makeCommentCis(taskCis.filter(_.`type` != "xlrelease.Dependency")) else Nil)
        }
    }

    val releaseAttachments: Seq[Ci] = generateAttachments(1, release.id)
    val activityLogs = generateActivityLogs(10, release.id)


    phases ++ cis  ++ releaseAttachments ++ activityLogs
  }

  private def makeCommentCis(parentIds: Seq[Ci]): Seq[Ci] =
    parentIds.zipWithIndex.map { case (p, n) => Comment.buildComment(s"Comment$n", p.id) }


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

package com.xebialabs.xlrelease.generator

import com.typesafe.config.Config
import com.xebialabs.xlrelease.domain._
import com.xebialabs.xlrelease.generator.ReleasesAndFoldersGenerator._

import scala.annotation.tailrec
import scala.util.Random

object ReleasesAndFoldersGenerator {
  val phasesPerRelease = 5
  val tasksPerPhase = 10
  val dependentReleaseId = "Applications/ReleaseDependent"
}

class ReleasesAndFoldersGenerator {
  var releaseIdCounter = 0
  var attachmentIdCounter = 0
  val transaction = Math.abs(Random.nextInt())
  var createdFolderIds: Seq[String] = Seq()

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
    createReleaseContent(release, generateComments = false, Seq.empty) :+ release
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


  def generateDependencyTrees(dependencyTreeAmount: Int, dependencyTreeDepth: Int, dependencyTreeBreadth: Int)
                             (implicit config: Config): Seq[Seq[Ci]] = {

    def generateDependencyTree(currentTree: Int, currentDepth: Int, dependencyTreeDepth: Int, dependencyTreeBreadth: Int)
                              (implicit config: Config): (Seq[Seq[Ci]], Seq[String]) = {
      if (currentDepth > dependencyTreeDepth) {
        (Seq.empty, Seq.empty)
      } else {
        val (childCis, targetCis) = generateDependencyTree(currentTree, currentDepth + 1, dependencyTreeDepth, dependencyTreeBreadth)

        val (cis, _) = generateReleases(if (currentDepth == 0) 1 else dependencyTreeBreadth, "IN_PROGRESS",
          (n) => s"Tree $currentTree release (depth: $currentDepth, number: $n)",
          genComments = false,
          dependsOn = targetCis
        )

        (childCis ++ cis, cis.flatten.filter(_.`type` == "xlrelease.GateTask").map(_.id))
      }
    }

    (1 to dependencyTreeAmount).flatMap { i =>
      generateDependencyTree(i, 0, dependencyTreeDepth, dependencyTreeBreadth)._1
    }
  }

  def generateReleases(amount: Int, status: String, titleGenerator: (Int) => String,
                       genComments: Boolean,
                       dependsOn: Seq[String] = Seq(dependentReleaseId))
                      (implicit config: Config): (Seq[Seq[Ci]], Seq[String]) = {
    val releases = (1 to amount).map { n =>
      val releaseNumber = incrementReleaseIdCounterAndGet()
      val folderId = if (createdFolderIds.isEmpty) {
        "Applications"
      } else {
        createdFolderIds(releaseNumber % createdFolderIds.size)
      }

      Release.build(s"$folderId/Release_${transaction}_$releaseNumber", titleGenerator(n), status, n, amount)
    }

    releases.map(release => createReleaseContent(release, genComments, dependsOn) :+ release) -> releases.map(_.id)
  }

  def generateAttachments(amount: Int, containerId: String)(implicit config: Config): Seq[Ci] = {
    for (_ <- 1 to amount) yield Attachment.build(s"Attachment${incrementAttachmentIdCounterAndGet()}", containerId)
  }

  def generateActivityLogs(amount: Int, releaseId: String): Seq[Ci] = {
    val directory = ActivityLogDirectory.build(releaseId)
    val entries = for (i <- 1 to amount) yield ActivityLogEntry.build(directory.id, message = s"Did some activity $i")
    List(directory) ++ entries
  }

  def generateFolders(amount: Int, levels: Int): Seq[Ci] = {

    @tailrec
    def createFolders(parentIds: Seq[String], amount: Int, level: Int, allCreatedFolders: Seq[Folder] = Seq()): Seq[Folder] = {
      if (level == 0) {
        allCreatedFolders
      } else {

        val foldersOnGivenLevel = for (i <- 1 to amount; parentId <- parentIds) yield {
          val folderName = s"${getFolderName(parentId)}_$i"
          val folderId = s"$parentId/$folderName"
          Folder.build(folderId, folderName)
        }

        val idsOnGivenLevel = foldersOnGivenLevel.map(_.id)
        createdFolderIds = createdFolderIds ++ idsOnGivenLevel

        createFolders(idsOnGivenLevel, amount, level - 1, allCreatedFolders ++ foldersOnGivenLevel)
      }
    }

    def getFolderName(folderId: String): String = {
      if (folderId == "Applications") {
        "Folder"
      } else if (folderId.contains("/")) {
        folderId.substring(folderId.lastIndexOf("/") + 1, folderId.length)
      } else {
        folderId
      }
    }

    val folders: Seq[Ci] = createFolders(Seq("Applications"), amount, levels).sortBy((ci) => ci.id)

    val activityLogs = folders.map(f => ActivityLogDirectory.build(f.id))
    val teams = folders.filter(_.id.matches("Applications/Folder_\\d+")).map(f => Team.build(f.id))

    folders ++ activityLogs ++ teams
  }

  private def createReleaseContent(release: Release, generateComments: Boolean, dependsOn: Seq[String])(implicit config: Config): Seq[Ci] = {
    val phaseNumbers = 1 to phasesPerRelease
    val phases: Seq[Phase] = phaseNumbers.map(n =>
      Phase.build(s"Phase$n", release.id, phaseStatus(release, n)))

    val cis: Seq[Ci] = phases.zip(phaseNumbers).flatMap {
      case (phase, phaseNumber) =>
        (1 to tasksPerPhase).flatMap { taskNumber =>
          val taskCis = makeTaskCis(phase, phaseNumber, taskNumber, release.id, dependsOn)
          taskCis ++ (if (generateComments) makeCommentCis(taskCis.filter(acceptsComment)) else Nil)
        }
    }

    val releaseAttachments: Seq[Ci] = generateAttachments(1, release.id)
    val activityLogs = generateActivityLogs(10, release.id)

    phases ++ cis ++ releaseAttachments ++ activityLogs
  }

  private def acceptsComment(ci: Ci): Boolean = {
    ci.`type` != "xlrelease.Dependency" && ci.`type` != "xlrelease.Attachment"
  }

  private def makeCommentCis(parentIds: Seq[Ci]): Seq[Ci] =
    parentIds.zipWithIndex.map { case (p, n) => Comment.buildComment(s"Comment$n", p.id) }


  private def makeTaskCis(phase: Phase, phaseNumber: Int, taskNumber: Int, releaseId: String, dependsOn: Seq[String])(implicit config: Config): Seq[Ci] = {
    if (isFirstTaskOfPhase(taskNumber)) {
      val attachment = generateAttachments(1, releaseId).head
      val task = Task.build(s"Task$taskNumber", phase.id, taskStatus(phase, taskNumber), attachments = List(attachment.id))
      Seq(task, attachment)
    } else if (isLastTaskOfRelease(phaseNumber, taskNumber)) {
      val task = Task.buildGate(s"Task$taskNumber", phase.id, taskStatus(phase, taskNumber))
      dependsOn.view.zipWithIndex.map {
        case (targetId, dependencyIndex) => Dependency.build(s"Dependency$dependencyIndex", task.id, targetId)
      } :+ task
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
      case "IN_PROGRESS" => if (phaseNumber == 1) "IN_PROGRESS" else "PLANNED"
      case _ => release.status
    }
  }

  private def taskStatus(phase: Phase, taskNumber: Int): String = {
    phase.status match {
      case "IN_PROGRESS" => if (taskNumber == 1) "IN_PROGRESS" else "PLANNED"
      case _ => phase.status
    }
  }
}

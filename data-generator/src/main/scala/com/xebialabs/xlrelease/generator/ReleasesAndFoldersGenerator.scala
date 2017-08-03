package com.xebialabs.xlrelease.generator

import com.typesafe.config.Config
import com.xebialabs.xlrelease.domain._
import com.xebialabs.xlrelease.generator.ReleasesAndFoldersGenerator._

import scala.annotation.tailrec
import scala.util.Random

object ReleasesAndFoldersGenerator {
  val phasesPerRelease = 5
  val tasksPerPhase = 10
  val automatedTasksPerPhase = 1
  val dependentReleaseId = "Applications/ReleaseDependent"
}

class ReleasesAndFoldersGenerator {
  val transaction: Int = Math.abs(Random.nextInt())

  var attachmentIdCounter = 0
  var releaseIdCounter = 0
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

  def generateAutomatedTemplates(amount: Int, genComments: Boolean = false)(implicit config: Config): Seq[Seq[Ci]] = {
    createReleases(amount, "TEMPLATE", (n) => s"Stress test automated template release $n", automated = true, genComments = genComments).map { template =>
      val cis = createReleaseContent(template, tasksPerPhase = automatedTasksPerPhase, generateComments = genComments, automated = true)
      val releaseTrigger = ReleaseTrigger.build(template.id, "Trigger1", s"${template.title} $${triggerTime}", enabled = true)
      Seq(template) ++ cis ++ Seq(releaseTrigger)
    }
  }

  def generateActiveReleases(amount: Int, genComments: Boolean = false)(implicit config: Config): Seq[Seq[Ci]] = {
    generateReleases(amount, "IN_PROGRESS", (n) => s"Stress test active release $n", genComments)._1
  }

  def generateDependentRelease()(implicit config: Config): Seq[Ci] = {
    val release = Release.build(dependentReleaseId, "Stress test Dependent release", "PLANNED", 1, 1)
    createReleaseContent(release, generateComments = false) :+ release
  }

  def generateDepRelease(relIds: Seq[String], numberOfRel: Int): Seq[Seq[Ci]] = {
    (1 to numberOfRel).zip(relIds).map { case (_, relId) =>
      val releaseNumber = incrementReleaseIdCounterAndGet()
      val release = Release.build(s"Applications/Release_${transaction}_$releaseNumber",
        s"Dependent release #$releaseNumber", "IN_PROGRESS", releaseNumber, numberOfRel)
      createDepRelContent(release, relId) :+ release
    }
  }

  private def createDepRelContent(r: Release, depRelId: String): Seq[Ci] = {
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

        val (cis, _) = generateReleases(if (currentDepth == 0) 1 else dependencyTreeBreadth, "PLANNED",
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

  private def generateReleases(amount: Int, status: String, titleGenerator: (Int) => String,
                               genComments: Boolean,
                               dependsOn: Seq[String] = Seq(dependentReleaseId))
                              (implicit config: Config): (Seq[Seq[Ci]], Seq[String]) = {
    val releases = createReleases(amount, status, titleGenerator, automated = false, genComments, dependsOn)

    releases.map(release => createReleaseContent(release, generateComments = genComments, dependsOn = dependsOn) :+ release) -> releases.map(_.id)
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

    val folders: Seq[Folder] = createFolders(Seq("Applications"), amount, levels).sortBy((ci) => ci.id)

    val activityLogs = folders.map(f => ActivityLogDirectory.build(f.id))
    val teams = folders
      .filter(_.id.matches("Applications/Folder_\\d+"))
      .flatMap(f => {
        val folderIndex = f.id.substring(f.id.lastIndexOf("_") + 1)
        Seq(
          releaseAdminTeam(f.id, Seq("admin", s"user$folderIndex")),
          templateOwnerTeam(f.id, Seq("admin", s"user$folderIndex")),
          folderOwnerTeam(f.id, Seq("admin", s"user$folderIndex")),
          Team.build(f.id, "Viewers", Seq("viewer"), Seq("folder#view", "release#view", "template#view"))
        )
      })

    folders ++ activityLogs ++ teams
  }

  private def releaseAdminTeam(containerId: String, members: Seq[String]) =
    Team.build(containerId, "Release Admin", members, Seq(
      "release#view",
      "release#edit",
      "release#edit_security",
      "release#start",
      "release#abort",
      "release#edit_task",
      "release#reassign_task"
    ))

  private def templateOwnerTeam(containerId: String, members: Seq[String]) =
    Team.build(containerId, "Template Owner", members, Seq(
      "template#create_release",
      "template#view",
      "template#edit",
      "template#edit_security",
      "template#edit_triggers"
    ))

  private def folderOwnerTeam(containerId: String, members: Seq[String]) =
    Team.build(containerId, "Folder Owner", members, Seq(
      "folder#view",
      "folder#edit",
      "folder#edit_security"
    ))

  private def createReleases(amount: Int,
                             status: String,
                             titleGenerator: (Int) => String,
                             automated: Boolean,
                             genComments: Boolean,
                             dependsOn: Seq[String] = Seq(dependentReleaseId))
                            (implicit config: Config): (Seq[Release]) = {
    (1 to amount).map { n =>
      val releaseNumber = incrementReleaseIdCounterAndGet()
      val folderId = if (createdFolderIds.isEmpty) {
        "Applications"
      } else {
        createdFolderIds(releaseNumber % createdFolderIds.size)
      }

      Release.build(s"$folderId/Release_${transaction}_$releaseNumber", titleGenerator(n), status, n, amount, allowConcurrentReleasesFromTrigger = !automated)
    }
  }

  private def createReleaseContent(release: Release, phasesPerRelease: Int = phasesPerRelease,
                                   tasksPerPhase: Int = tasksPerPhase, generateComments: Boolean,
                                   dependsOn: Seq[String] = Seq.empty, automated: Boolean = false)(implicit config: Config): Seq[Ci] = {
    val phaseNumbers = 1 to phasesPerRelease
    val phases: Seq[Phase] = phaseNumbers.map(n =>
      Phase.build(s"Phase$n", release.id, phaseStatus(release, n)))

    val cis: Seq[Ci] = phases.zip(phaseNumbers).flatMap {
      case (phase, phaseNumber) =>
        (1 to tasksPerPhase).flatMap { taskNumber =>
          val taskCis = makeTaskCis(phase, phaseNumber, taskNumber, automated, release.id, dependsOn)
          taskCis ++ (if (generateComments) makeCommentCis(taskCis.filter(acceptsComment)) else Nil)
        }
    }

    val releaseAttachments: Seq[Ci] = makeAttachments(1, release.id)
    val activityLogs = makeActivityLogs(10, release.id)

    // phases ++ cis ++ releaseAttachments ++ activityLogs
    phases ++ cis ++ activityLogs
  }

  private def acceptsComment(ci: Ci): Boolean = {
    ci.`type` != "xlrelease.Dependency" && ci.`type` != "xlrelease.Attachment"
  }

  private def makeCommentCis(parentIds: Seq[Ci]): Seq[Ci] =
    parentIds.zipWithIndex.map { case (p, n) => Comment.buildComment(s"Comment$n", p.id) }

  private def makeTaskCis(phase: Phase, phaseNumber: Int, taskNumber: Int, automated: Boolean, releaseId: String, dependsOn: Seq[String])
                         (implicit config: Config): Seq[Ci] = {
    if (isFirstTaskOfPhase(taskNumber)) {
      //val attachment = makeAttachments(1, releaseId).head
      val task = makeTask(phase, taskNumber, automated, List.empty)) // List(attachment.id))
      Seq(task) //, attachment)
    } else if (isLastTaskOfRelease(phaseNumber, taskNumber)) {
      val task = Task.buildGate(s"Task$taskNumber", phase.id, taskStatus(phase, taskNumber))
      dependsOn.view.zipWithIndex.map {
        case (targetId, dependencyIndex) => Dependency.build(s"Dependency$dependencyIndex", task.id, targetId)
      } :+ task
    } else {
      Seq(makeTask(phase, taskNumber, automated, List()))
    }
  }

  private def makeTask(phase: Phase, taskNumber: Int, automated: Boolean, attachments: List[String]): AbstractTask = {
    val automatedScript =
      """
import time
import uuid

for n in range(0, 100):
    print "Automated message {}, {}".format(n, str(uuid.uuid4()))
    time.sleep(0.5)

      """
    if (automated) {
      ScriptTask.build(s"Task$taskNumber", phase.id, taskStatus(phase, taskNumber), attachments = attachments, script = automatedScript)
    } else {
      Task.build(s"Task$taskNumber", phase.id, taskStatus(phase, taskNumber), attachments = attachments)
    }
  }

  private def makeAttachments(amount: Int, containerId: String)(implicit config: Config): Seq[Ci] = {
    for (_ <- 1 to amount) yield Attachment.build(s"Attachment${incrementAttachmentIdCounterAndGet()}", containerId)
  }

  private def makeActivityLogs(amount: Int, releaseId: String): Seq[Ci] = {
    val directory = ActivityLogDirectory.build(releaseId)
    val entries = for (i <- 1 to amount) yield ActivityLogEntry.build(directory.id, message = s"Did some activity $i")
    List(directory) ++ entries
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

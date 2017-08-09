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

class ReleasesAndFoldersGenerator(implicit config: Config) {
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

  def generateMutableReleases(amount: Int, genComments: Boolean = false): Seq[ReleaseAndRelatedCis] = {
    generateReleases(amount, "PLANNED", n => s"[BULK] Release for starting/aborting $n", genComments = genComments)
  }

  def generateActiveReleases(amount: Int, genComments: Boolean = false): Seq[ReleaseAndRelatedCis] = {
    generateReleases(amount, "IN_PROGRESS", (n) => s"Stress test active release $n", genComments = genComments)
  }

  def generateCompletedReleases(amount: Int, genComments: Boolean = false): Seq[ReleaseAndRelatedCis] = {
    generateReleases(amount, "COMPLETED", (n) => s"Stress test completed release $n", genComments = genComments)
  }

  def generateTemplateReleases(amount: Int, genComments: Boolean = false): Seq[ReleaseAndRelatedCis] = {
    generateReleases(amount, "TEMPLATE", (n) => s"Stress test template release $n", genComments = genComments)
  }

  def generateAutomatedTemplates(amount: Int, genComments: Boolean = false): Seq[ReleaseAndRelatedCis] = {
    val templatesAndOtherCis = generateReleases(amount, "TEMPLATE", (n) => s"Stress test automated template release $n",
      automated = true, genComments = genComments)

    templatesAndOtherCis.foreach { templateAndOtherCis =>
      val template = templateAndOtherCis.release
      val releaseTrigger = ReleaseTrigger.build(template.id, "Trigger1", s"${template.title} $${triggerTime}", enabled = true)
      template.releaseTriggers = Seq(releaseTrigger)
    }

    templatesAndOtherCis
  }

  def generateDependentRelease(): Release = {
    val (releases, _) = makeRelease(
      releaseId = dependentReleaseId,
      title = "Stress test Dependent release",
      status = "PLANNED",
      releaseNumber = 1,
      totalReleasesAmount = 1,
      automated = false,
      generateComments = false
    )
    releases
  }

  def generateReleasesDependingOn(releaseIdsToDependOn: Seq[String], numberOfReleases: Int): Seq[Release] = {
    (1 to numberOfReleases).zip(releaseIdsToDependOn).map { case (_, releaseIdToDependOn) =>
      val releaseNumber = incrementReleaseIdCounterAndGet()

      val release = Release.build(s"Applications/Release_${transaction}_$releaseNumber",
        s"Dependent release #$releaseNumber", "IN_PROGRESS", releaseNumber, numberOfReleases)

      val inProgress: Boolean => String = b => if (b) "IN_PROGRESS" else "PLANNED"
      release.phases = (1 to 10).map { i =>
        val phase = Phase.build(s"Phase$i", release.id, inProgress(i == 1))
        phase.tasks = (1 to 10).map { j =>
          val task = GateTask.build(s"Task$j", phase.id, inProgress(j == 1 && i == 1))
          task.dependencies = Seq(Dependency.build("Dependency", task.id, releaseIdToDependOn))
          task
        }
        phase
      }

      release
    }
  }

  def generateDependencyTrees(dependencyTreeAmount: Int, dependencyTreeDepth: Int,
                              dependencyTreeBreadth: Int): Seq[ReleaseAndRelatedCis] = {

    type AllReleasesAndTopLevelIds = (Seq[ReleaseAndRelatedCis], Seq[String])

    def generateDependencyTree(currentTree: Int, currentDepth: Int, maxDepth: Int, treeBreadth: Int)
                              : AllReleasesAndTopLevelIds = {
      if (currentDepth > maxDepth) {
        (Seq.empty, Seq.empty)
      } else {
        val (allChildReleases, directChildIds) = generateDependencyTree(currentTree, currentDepth + 1, maxDepth, treeBreadth)

        val releasesOnThisLevel: Seq[ReleaseAndRelatedCis] = generateReleases(
          amount = if (currentDepth == 0) 1 else treeBreadth,
          status = "PLANNED",
          titleGenerator = (n) => s"Tree $currentTree release (depth: $currentDepth, number: $n)",
          genComments = false,
          dependsOn = directChildIds
        )
        val gateIds = releasesOnThisLevel
          .flatMap(_.release.phases)
          .flatMap(_.tasks)
          .filter(_.isInstanceOf[GateTask])
          .map(_.id)

        (allChildReleases ++ releasesOnThisLevel, gateIds)
      }
    }

    (1 to dependencyTreeAmount).flatMap { i =>
      generateDependencyTree(i, 0, dependencyTreeDepth, dependencyTreeBreadth)._1
    }
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

  private def generateReleases(amount: Int,
                               status: String,
                               titleGenerator: (Int) => String,
                               automated: Boolean = false,
                               genComments: Boolean,
                               dependsOn: Seq[String] = Seq(dependentReleaseId))
                              : Seq[ReleaseAndRelatedCis] = {
    (1 to amount).map { n =>
      val releaseNumber = incrementReleaseIdCounterAndGet()
      val folderId = if (createdFolderIds.isEmpty) {
        "Applications"
      } else {
        createdFolderIds(releaseNumber % createdFolderIds.size)
      }
      val releaseId = s"$folderId/Release_${transaction}_$releaseNumber"

      val (release, activityLogs) = makeRelease(
        releaseId = releaseId,
        title = titleGenerator(n),
        status = status,
        releaseNumber = n,
        totalReleasesAmount = amount,
        automated = automated,
        generateComments = genComments,
        dependsOn = dependsOn
      )

      ReleaseAndRelatedCis(release, activityLogs)
    }
  }

  private def makeRelease(releaseId: String,
                          title: String,
                          status: String,
                          releaseNumber: Int,
                          totalReleasesAmount: Int,
                          automated: Boolean,
                          generateComments: Boolean,
                          dependsOn: Seq[String] = Seq(dependentReleaseId))
                         : (Release, Seq[ActivityLogCi]) = {
    val release = Release.build(releaseId, title, status, releaseNumber, totalReleasesAmount,
      allowConcurrentReleasesFromTrigger = !automated)

    val (phases, attachments, activityLogs) = makeReleaseContent(
      release,
      generateComments = generateComments,
      dependsOn = dependsOn,
      automated = automated
    )
    release.phases = phases
    release.attachments = attachments

    (release, activityLogs)
  }


  private def makeReleaseContent(release: Release, phasesPerRelease: Int = phasesPerRelease,
                                 tasksPerPhase: Int = tasksPerPhase, generateComments: Boolean,
                                 dependsOn: Seq[String] = Seq.empty, automated: Boolean = false)
                                : (Seq[Phase], Seq[Attachment], Seq[ActivityLogCi]) = {

    val phaseNumbers = 1 to phasesPerRelease
    val phases: Seq[Phase] = phaseNumbers.map(n =>
      Phase.build(s"Phase$n", release.id, phaseStatus(release, n)))

    val taskAttachments: Seq[Attachment] = phases.zip(phaseNumbers).flatMap {
      case (phase, phaseNumber) =>
        val tasksAndAttachments = (1 to tasksPerPhase).map { taskNumber =>
          val (task, attachmentOpt) = makeTaskAndMaybeAttachment(
            phase, phaseNumber, taskNumber, automated, release.id, dependsOn
          )
          if (generateComments) {
            task.comments = task.comments :+ makeComment(task.id)
          }
          (task, attachmentOpt)
        }
        val (tasks, attachmentOptions) = tasksAndAttachments.unzip
        phase.tasks = tasks

        attachmentOptions.flatten
    }

    val releaseAttachments: Seq[Attachment] = Seq(makeAttachments(release.id))
    val activityLogs = makeActivityLogs(10, release.id)

    (phases, taskAttachments ++ releaseAttachments, activityLogs)
  }

  private def makeComment(parentId: String): Comment =
    Comment.buildComment(s"Comment0", parentId)

  private def makeTaskAndMaybeAttachment(phase: Phase, phaseNumber: Int, taskNumber: Int,
                                         automated: Boolean, releaseId: String,
                                         dependsOn: Seq[String])
                                        : (AbstractTask, Option[Attachment]) = {
    if (isFirstTaskOfPhase(taskNumber)) {
      val attachment = makeAttachments(releaseId)
      val task = makeTask(phase, taskNumber, automated, Seq(attachment.id))
      (task, Some(attachment))
    } else if (isLastTaskOfRelease(phaseNumber, taskNumber)) {
      val task = GateTask.build(s"Task$taskNumber", phase.id, taskStatus(phase, taskNumber))
      dependsOn.zipWithIndex.map {
        case (targetId, dependencyIndex) =>
          val dependency = Dependency.build(s"Dependency$dependencyIndex", task.id, targetId)
          task.dependencies = task.dependencies :+ dependency
          dependency
      }
      (task, None)
    } else {
      (makeTask(phase, taskNumber, automated, Seq()), None)
    }
  }

  private def makeTask(phase: Phase, taskNumber: Int, automated: Boolean, attachments: Seq[String]): AbstractTask = {
    if (automated) {
      val automatedScript =
        """
import time
import uuid

for n in range(0, 100):
    print "Automated message {}, {}".format(n, str(uuid.uuid4()))
    time.sleep(0.5)

      """
      ScriptTask.build(s"Task$taskNumber", phase.id, taskStatus(phase, taskNumber), attachments = attachments, script = automatedScript)
    } else {
      Task.build(s"Task$taskNumber", phase.id, taskStatus(phase, taskNumber), attachments = attachments)
    }
  }

  private def makeAttachments(containerId: String): Attachment = {
    Attachment.build(s"Attachment${incrementAttachmentIdCounterAndGet()}", containerId)
  }

  private def makeActivityLogs(amount: Int, releaseId: String): Seq[ActivityLogCi] = {
    val directory = ActivityLogDirectory.build(releaseId)
    val entries = for (i <- 1 to amount) yield ActivityLogEntry.build(directory.id, message = s"Did some activity $i")
    Seq(directory) ++ entries
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

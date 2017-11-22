package com.xebialabs.xlrelease


import java.util

import org.threeten.bp.{LocalDateTime, ZoneId, ZonedDateTime}

import scala.language.implicitConversions
import scala.util.Random

package object domain {

  trait Ci {
    def id: String

    def `type`: String
  }

  trait PlanItem extends Ci {
    def title: String

    def status: String
  }

  case class User(password: String, username: String, email: String = "", fullName: String = "", loginAllowed: Boolean = true)

  case class Permission(role: Role, permissions: Seq[String])

  case class Role(id: Option[Int] = None, name: String)

  case class PUser(username: String, fullName: String = "")

  case class Principal(role: Role, principals: Seq[PUser])

  case class Release(id: String,
                     title: String,
                     status: String,
                     riskProfileId:String ="Configuration/riskProfiles/RiskProfileDefault",
                     scheduledStartDate: ZonedDateTime,
                     dueDate: ZonedDateTime,
                     queryableStartDate: ZonedDateTime,
                     queryableEndDate: ZonedDateTime,
                     startDate: ZonedDateTime,
                     endDate: Option[ZonedDateTime],
                     allowConcurrentReleasesFromTrigger: Boolean = true,
                     `type`: String = "xlrelease.Release",
                     var phases: Seq[Phase] = Seq(),
                     var attachments: Seq[Attachment] = Seq(),
                     var releaseTriggers: Seq[ReleaseTrigger] = Seq()) extends PlanItem

  case class Phase(id: String,
                   title: String,
                   `type`: String = "xlrelease.Phase",
                   color: String = "#009CDB",
                   status: String = "PLANNED",
                   var tasks: Seq[AbstractTask] = Seq()) extends PlanItem

  trait AbstractTask extends PlanItem {
    def attachments: Seq[String]
    var comments: Seq[Comment]
  }

  case class Task(id: String,
                  title: String,
                  `type`: String = "xlrelease.Task",
                  status: String,
                  attachments: Seq[String],
                  var comments: Seq[Comment] = Seq()
                 ) extends AbstractTask

  case class ScriptTask(id: String,
                        title: String,
                        `type`: String = "xlrelease.ScriptTask",
                        status: String = "PLANNED",
                        script: String,
                        attachments: Seq[String] = Seq(),
                        var comments: Seq[Comment] = Seq()
                       ) extends AbstractTask

  case class GateTask(id: String,
                      title: String,
                      `type`: String = "xlrelease.GateTask",
                      status: String = "PLANNED",
                      attachments: Seq[String] = Seq(),
                      var comments: Seq[Comment] = Seq(),
                      var dependencies: Seq[Dependency] = Seq()) extends AbstractTask

  case class Comment(id: String,
                     text: String,
                     `type`: String = "xlrelease.Comment"
                    ) extends Ci

  case class Dependency(id: String,
                        target: String,
                        `type`: String = "xlrelease.Dependency") extends Ci

  case class SpecialDay(id: String,
                        label: String,
                        color: String = "#c3d4ef",
                        `type`: String = "xlrelease.SpecialDay") extends Ci

  case class RiskProfile(id: String,
                        title: String,
                         riskProfileAssessors : Map[String, String] = Map(),
                         defaultProfile:Boolean = false,
                          `type`: String = "xlrelease.RiskProfile"
                          ) extends Ci

  case class Directory(id: String, `type`: String = "core.Directory") extends Ci

  case class HttpConnection(id: String, title: String, `type`: String = "configuration.HttpConnection") extends Ci

  case class Attachment(id: String, `type`: String = "xlrelease.Attachment") extends Ci

  case class ActivityLogEntry(id: String, username: String, activityType: String, message: String, eventTime: String, `type`: String = "xlrelease.ActivityLogEntry") extends Ci

  case class Folder(id: String,
                    title: String,
                    `type`: String = "xlrelease.Folder") extends Ci

  case class Team(id: String,
                  teamName: String,
                  members: Seq[String],
                  permissions: Seq[String],
                  `type`: String = "xlrelease.Team") extends Ci

  case class ReleaseTrigger(id: String,
                            title: String,
                            `type`: String = "time.Schedule",
                            pollType: String = "REPEAT",
                            periodicity: String = "300",
                            releaseTitle: String,
                            enabled: Boolean = false,
                            initialFire: Boolean = false) extends Ci


  case class ReleaseAndRelatedCis(release: Release, activityLogs: Seq[ActivityLogEntry])


  object Release {
    def build(title: String): Release = {
      if (!title.startsWith("Release"))
        throw new IllegalArgumentException("Release id/title should start with 'Release'")

      build(s"Applications/$title", title, "PLANNED", 0, 1,"Configuration/riskProfiles/RiskProfile"+"Default")
    }

    def build(id: String,
              title: String,
              status: String,
              releaseNumber: Double,
              releasesCount: Double,
              riskProfileId:String,
              allowConcurrentReleasesFromTrigger: Boolean = true): Release = {
      if (!id.matches("^Applications/(Folder.*/|ActivityLogs.*/)?Release.*$"))
        throw new IllegalArgumentException(s"Container id should start with 'Applications/Folder.../.../Release but starts with [$id]'")

      val firstDayOfYear = ZonedDateTime.of(LocalDateTime.of(2015, 1, 1, 9, 0), ZoneId.systemDefault)
      val offset = Math.floor(365.0 * releaseNumber / releasesCount).toInt % 365
      val start = firstDayOfYear.plusDays(offset)
      val end = start.plusDays(30)

      Release(id, title, status,riskProfileId,
        scheduledStartDate = start,
        dueDate = end,
        queryableStartDate = start,
        queryableEndDate = end,
        startDate = start,
        endDate = if (status == "COMPLETED") Some(end) else None,
        allowConcurrentReleasesFromTrigger
      )
    }

  }

  object ActivityLogDirectory {
    def build(containerId: String): Directory = {
      Directory(containerId.replace("Applications/", "Applications/ActivityLogs/"))
    }
  }

  object ActivityLogEntry {
    def build(releaseId: String,
              username: String = "admin",
              activityType: String = "IMPORTANT",
              message: String = "Did some activity",
              eventTime: String = LocalDateTime.now.toString): ActivityLogEntry = {
      val directoryId = releaseId.replace("Applications/", "Applications/ActivityLogs/")
      ActivityLogEntry(directoryId + s"/Activity${System.currentTimeMillis + Random.nextInt}",
        username, activityType, message, eventTime)
    }
  }

  object Phase {
    def build(title: String,
              releaseId: String,
              status: String = "PLANNED"
             ): Phase = {
      if (!title.startsWith("Phase"))
        throw new IllegalArgumentException("Phase id/title should start with 'Phase'")

      Phase(s"$releaseId/$title", title, status = status)
    }
  }

  object RiskProfile {
    def build(id: String,
              title: String,
              riskProfileAssessors : Map[String, String] = Map(),
                defaultProfile:Boolean = false,
                `type`: String = "xlrelease.RiskProfile"
              ): RiskProfile = {

      RiskProfile(id, title)
    }
  }

  object Task {
    def build(title: String, containerId: String, status: String = "COMPLETED", attachments: Seq[String] = Seq()): Task = {
      if (!title.startsWith("Task")) throw new IllegalArgumentException("Task id/title should start with 'Task'")
      Task(s"$containerId/$title", title, status = status, attachments = attachments)
    }
  }

  object ScriptTask {
    def build(title: String, containerId: String, status: String = "COMPLETED", attachments: Seq[String] = Seq(), script: String): ScriptTask = {
      ScriptTask(id = s"$containerId/$title",
        title = title,
        status = status,
        attachments = attachments,
        script = script)
    }
  }

  object GateTask {
    def build(title: String, containerId: String, status: String = "COMPLETED", attachments: Seq[String] = Seq()): GateTask = {
      GateTask(id = s"$containerId/$title",
        title = title,
        status = status,
        attachments = attachments)
    }
  }

  object Comment {

    def buildComment(title: String, containerId: String): Comment =
      if (!title.startsWith("Comment")) throw new IllegalArgumentException("Comment id/title should start with 'Comment'")
      else Comment(s"$containerId/$title", generateText())

    def generateText(size: Int = 0): String = {
      // 100.000 chars per comment which should be ~291K per comment which is ~29MB per release
      val str = Random.nextString(100)
      val sb = new StringBuilder()
      (0 until 1000).foreach(_ => sb.append(str))
      sb.toString()
    }
  }

  object Dependency {
    def build(title: String, containerId: String, target: String): Dependency = {
      Dependency(s"$containerId/$title", target)
    }
  }

  object Attachment {

    def build(title: String, containerId: String, sizeInKb: Int = 500): Attachment = {
      // The attachment content is generated on the server side, the size to generate
      // is parsed from the attachment ID.
      Attachment(s"$containerId/${title}_${sizeInKb}KB")
    }
  }

  object Folder {
    def build(id: String, title: String): Folder = {
      Folder(id, title)
    }
  }

  object Team {
    def build(containerId: String, containerIndex: String, teamName: String,
              members: Seq[String], permissions: Seq[String]): Team = {
      Team(s"$containerId/Team${teamName}_$containerIndex", teamName, members, permissions)
    }
  }

  object ReleaseTrigger {
    def build(containerId: String, title: String, releaseTitle: String, enabled: Boolean = false, initialFire: Boolean = false): ReleaseTrigger = {
      ReleaseTrigger(id = s"$containerId/$title", title = title, releaseTitle = releaseTitle, enabled = enabled, initialFire = initialFire)
    }
  }

  implicit def users2pusers(uu: Seq[User]): Seq[PUser] = uu.map(u => PUser(u.username, u.fullName))
}

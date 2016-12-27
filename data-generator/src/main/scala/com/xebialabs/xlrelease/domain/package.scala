package com.xebialabs.xlrelease


import com.typesafe.config.Config
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
                     scheduledStartDate: ZonedDateTime,
                     dueDate: ZonedDateTime,
                     queryableStartDate: ZonedDateTime,
                     queryableEndDate: ZonedDateTime,
                     startDate: ZonedDateTime,
                     endDate: Option[ZonedDateTime],
                     allowConcurrentReleasesFromTrigger: Boolean = true,
                     `type`: String = "xlrelease.Release") extends PlanItem

  case class Phase(id: String,
                   title: String,
                   `type`: String = "xlrelease.Phase",
                   color: String = "#009CDB",
                   status: String = "PLANNED") extends PlanItem

  abstract class AbstractTask(id: String,
                              title: String,
                              `type`: String,
                              status: String = "PLANNED",
                              attachments: List[String] = List()
                             ) extends PlanItem

  case class Task(id: String,
                  title: String,
                  `type`: String = "xlrelease.Task",
                  status: String,
                  attachments: List[String]
                 ) extends AbstractTask(id, title, `type`, status, attachments)

  case class ScriptTask(id: String,
                        title: String,
                        `type`: String = "xlrelease.ScriptTask",
                        status: String = "PLANNED",
                        attachments: List[String],
                        script: String) extends AbstractTask(id, title, "xlrelease.ScriptTask", status, attachments)

  case class Comment(id: String,
                     text: String,
                     `type`: String = "xlrelease.Comment"
                    ) extends Ci

  case class Dependency(id: String,
                        target: String,
                        `type`: String = "xlrelease.Dependency") extends Ci

  case class SpecialDay(id: String,
                        label: String,
                        date: String,
                        color: String = "#c3d4ef",
                        `type`: String = "xlrelease.SpecialDay") extends Ci

  case class Directory(id: String, `type`: String = "core.Directory") extends Ci

  case class HttpConnection(id: String, title: String, `type`: String = "configuration.HttpConnection") extends Ci

  case class Attachment(id: String, fileUri: String, `type`: String = "xlrelease.Attachment") extends Ci

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
                            periodicity: String = "120",
                            releaseTitle: String,
                            enabled: Boolean = false,
                            initialFire: Boolean = false) extends Ci

  object Release {
    def build(title: String): Release = {
      if (!title.startsWith("Release"))
        throw new IllegalArgumentException("Release id/title should start with 'Release'")

      build(s"Applications/$title", title, "PLANNED", 0, 1)
    }

    def build(id: String,
              title: String,
              status: String,
              releaseNumber: Double,
              releasesCount: Double,
              allowConcurrentReleasesFromTrigger: Boolean = true): Release = {
      if (!id.matches("^Applications/(Folder.*/|ActivityLogs.*/)?Release.*$"))
        throw new IllegalArgumentException(s"Container id should start with 'Applications/Folder.../.../Release but starts with [$id]'")

      val firstDayOfYear = ZonedDateTime.of(LocalDateTime.of(2015, 1, 1, 9, 0), ZoneId.systemDefault)
      val offset = Math.floor(365.0 * releaseNumber / releasesCount).toInt % 365
      val start = firstDayOfYear.plusDays(offset)
      val end = start.plusDays(30)

      Release(id, title, status,
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
    def build(directoryId: String,
              username: String = "admin",
              activityType: String = "IMPORTANT",
              message: String = "Did some activity",
              eventTime: String = LocalDateTime.now.toString): ActivityLogEntry = {

      ActivityLogEntry(directoryId + s"/Activity${System.currentTimeMillis + Random.nextInt}", username, activityType, message, eventTime)
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

  object Task {

    def build(title: String, containerId: String, status: String = "COMPLETED", attachments: List[String] = List()): Task = {
      if (!title.startsWith("Task")) throw new IllegalArgumentException("Task id/title should start with 'Task'")
      Task(s"$containerId/$title", title, status = status, attachments = attachments)
    }

    def buildGate(title: String, containerId: String, status: String = "COMPLETED"): Task =
      build(title, containerId, status).copy(`type` = "xlrelease.GateTask")
  }

  object ScriptTask {
    def build(title: String, containerId: String, status: String = "COMPLETED", attachments: List[String] = List(), script: String): ScriptTask = {
      ScriptTask(id = s"$containerId/$title",
        title = title,
        status = status,
        attachments = attachments,
        script = script)
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
      (0 until 1000).foreach(x => sb.append(str))
      sb.toString()
    }
  }

  object Dependency {
    def build(title: String, containerId: String, target: String): Dependency = {
      Dependency(s"$containerId/$title", target)
    }
  }

  object Attachment {

    // We generate an attachment content by pointing to 'xlrelease-plugins.js' file which is downloadable
    // from XL Release itself without authentication and takes around 150 Kb.
    def xlrAttachmentUrl(baseUrl: String) = s"$baseUrl/ui-extensions/js/xlrelease-plugins.js"

    def build(title: String, containerId: String)(implicit config: Config): Attachment = {
      val attachmentsBaseUrl = config.getString("xl.data-generator.baseUrl")
      Attachment(s"$containerId/$title", xlrAttachmentUrl(attachmentsBaseUrl))
    }
  }

  object Folder {
    def build(id: String, title: String): Folder = {
      Folder(id, title)
    }
  }

  object Team {
    def build(containerId: String): Team = {
      Team(s"$containerId/TeamViewers", "Viewers", Seq("viewer"), Seq("folder#view", "release#view", "template#view"))
    }
  }

  object ReleaseTrigger {
    def build(containerId: String, title: String, releaseTitle: String, enabled: Boolean = false, initialFire: Boolean = false): ReleaseTrigger = {
      ReleaseTrigger(id = s"$containerId/$title", title = title, releaseTitle = releaseTitle, enabled = enabled, initialFire = initialFire)
    }
  }

  implicit def users2pusers(uu: Seq[User]): Seq[PUser] = uu.map(u => PUser(u.username, u.fullName))
}

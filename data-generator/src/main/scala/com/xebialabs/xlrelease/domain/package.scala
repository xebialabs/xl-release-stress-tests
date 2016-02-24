package com.xebialabs.xlrelease


import com.typesafe.config.Config
import org.threeten.bp.{LocalDateTime, ZoneId, ZonedDateTime}

import scala.language.implicitConversions

package object domain {

  trait Ci {
    def id: String
    def `type`: String
  }

  trait PlanItem extends Ci {
    def title: String
    def status: String
  }

  case class User(password: String, username: String, email: String = "", fullName: String ="", loginAllowed: Boolean = true)
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
                      endDate: Option[ZonedDateTime],
                      `type`: String = "xlrelease.Release") extends PlanItem

  case class Phase(id: String,
                   title: String,
                   `type`: String = "xlrelease.Phase",
                   color: String = "#009CDB",
                   status: String = "PLANNED") extends PlanItem

  case class Task(id: String,
                  title: String,
                  `type`: String = "xlrelease.Task",
                  status: String = "PLANNED",
                  attachments: List[String] = List()
                  ) extends PlanItem

  case class Dependency(id: String,
                        target: String,
                        `type`: String = "xlrelease.Dependency") extends Ci

  case class SpecialDay(id: String,
                        label: String,
                        date: String,
                        color: String= "#c3d4ef",
                        `type`: String = "xlrelease.SpecialDay") extends Ci

  case class Directory(id: String, `type`: String = "core.Directory") extends Ci

  case class HttpConnection(id: String, title: String, `type`: String = "configuration.HttpConnection") extends Ci

  case class Attachment(id: String, fileUri: String, `type`: String = "xlrelease.Attachment") extends Ci

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
              releasesCount: Double): Release = {
      if (!id.startsWith("Applications/Release"))
        throw new IllegalArgumentException("Release id should start with 'Applications/Release'")

      val firstDayOfYear = ZonedDateTime.of(LocalDateTime.of(2015, 1, 1, 9, 0), ZoneId.systemDefault)
      val offset = Math.floor(365.0 * releaseNumber / releasesCount).toInt % 365
      val start = firstDayOfYear.plusDays(offset)
      val end = start.plusDays(30)

      Release(id, title, status,
        scheduledStartDate = start,
        dueDate = end,
        queryableStartDate = start,
        queryableEndDate = end,
        endDate = if (status == "COMPLETED") Some(end) else None)
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

  object Dependency {
    def build(title: String, containerId: String, target: String): Dependency = {
      Dependency(s"$containerId/$title", target)
    }
  }

  object Attachment {

    // We generate an attachment content by pointing to 'xlrelease.js' file which is downloadable
    // from XL Release itself without authentication and takes around 350 Kb.
    def xlrAttachmentUrl(baseUrl: String) = s"$baseUrl/static/0/js/xlrelease.js"

    def build(title: String, containerId: String)(implicit config: Config): Attachment = {
      val attachmentsBaseUrl = config.getString("xl.data-generator.baseUrl")
      Attachment(s"$containerId/$title", xlrAttachmentUrl(attachmentsBaseUrl))
    }
  }

  implicit def users2pusers(uu: Seq[User]): Seq[PUser] = uu.map(u => PUser(u.username, u.fullName))
}

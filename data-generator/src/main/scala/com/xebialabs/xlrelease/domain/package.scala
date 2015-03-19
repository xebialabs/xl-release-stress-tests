package com.xebialabs.xlrelease

import org.joda.time.DateTime

import scala.language.implicitConversions

package object domain {

  trait Ci {
    def id: String
    def `type`: String
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
                      scheduledStartDate: DateTime,
                      dueDate: DateTime,
                      queryableStartDate: DateTime,
                      queryableEndDate: DateTime,
                      `type`: String = "xlrelease.Release") extends Ci

  case class Phase(id: String,
                   title: String,
                   `type`: String = "xlrelease.Phase",
                   color: String = "#009CDB",
                   status: String = "PLANNED") extends Ci

  case class Task(id: String,
                  title: String,
                  `type`: String = "xlrelease.Task",
                  status: String = "PLANNED"
                  ) extends Ci


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

      val offset = Math.floor(365.0 * releaseNumber / releasesCount).toInt % 365
      val start = new DateTime(2015, 1, 1, 9, 0).plusDays(offset)
      val end = start.plusDays(30)

      Release(id, title, status,
        scheduledStartDate = start,
        dueDate = end,
        queryableStartDate = start,
        queryableEndDate = end)
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

    def build(title: String, containerId: String, status: String = "PLANNED"): Task = {
      if (!title.startsWith("Task")) throw new IllegalArgumentException("Task id/title should start with 'Task'")
      Task(s"$containerId/$title", title, status = "COMPLETED")
    }

    def buildGate(title: String, containerId: String): Task =
      build(title, containerId).copy(`type` = "xlrelease.GateTask")
  }

  implicit def users2pusers(uu: Seq[User]): Seq[PUser] = uu.map(u => PUser(u.username, u.fullName))
}

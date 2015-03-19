package com.xebialabs.xlrelease


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

  object Release {

    def build(title: String): Release = {
      if (!title.startsWith("Release")) throw new IllegalArgumentException("Release id/title should start with 'Release'")
      Release(s"Applications/$title", title)
    }

  }
  case class Release(id: String, title: String, status: String = "PLANNED", `type`: String = "xlrelease.Release") extends Ci

  object Phase {
    def build(title: String,
              releaseId: String,
              status: String = "PLANNED"
             ): Phase = {
      if (!title.startsWith("Phase")) throw new IllegalArgumentException("Phase id/title should start with 'Phase'")
      Phase(s"$releaseId/$title", title, status = status)
    }
  }

  case class Phase(id: String,
                   title: String,
                   `type`: String = "xlrelease.Phase",
                   color: String = "#009CDB",
                   status: String = "PLANNED") extends Ci

  object Task {

    def build(title: String, containerId: String, status: String = "PLANNED"): Task = {
      if (!title.startsWith("Task")) throw new IllegalArgumentException("Task id/title should start with 'Task'")
      Task(s"$containerId/$title", title, status = "COMPLETED")
    }

    def buildGate(title: String, containerId: String): Task =
      build(title, containerId).copy(`type` = "xlrelease.GateTask")
  }

  case class Task(id: String,
                  title: String,
                  `type`: String = "xlrelease.Task",
                  status: String = "PLANNED") extends Ci


  implicit def users2pusers(uu: Seq[User]): Seq[PUser] = uu.map(u => PUser(u.username, u.fullName))
}

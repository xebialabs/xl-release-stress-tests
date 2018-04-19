package com.xebialabs.xlrelease.stress.client

import java.nio.file.Paths

import com.xebialabs.xlrelease.stress.client.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.domain.Permission.{CreateRelease, CreateTemplate, CreateTopLevelFolder}
import com.xebialabs.xlrelease.stress.domain._
import cats._
import cats.implicits._
import cats.syntax._
import com.xebialabs.xlrelease.stress.{client, engine}
import com.xebialabs.xlrelease.stress.domain.Member.RoleMember
import com.xebialabs.xlrelease.stress.domain.Team.{releaseAdmin, templateOwner}
import com.xebialabs.xlrelease.stress.engine.HiProgram
import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.nondeterminism._
import freestyle.free.logging._
import freestyle.free.loggingJVM.log4s.implicits._

object TestScenarios {

  def setup(template: Template, numUsers: Int)(implicit API: engine.API): HiProgram[(Role, Template.ID)] = {
    import API.client._

    for {
      admin <- users.admin()
      role <- createUsers(numUsers) >>= createGlobalRole("superDuperRole")
      members: Seq[Member] = Seq(RoleMember(role.rolename))
      templateId <- releases.importTemplate(admin, template)
      _ <- releases.setTemplateScriptUser(admin, templateId, scriptUser = Some(admin.user))
      _ <- releases.setTemplateTeams(admin, templateId, Seq(templateOwner(members), releaseAdmin(members)))
    } yield (role, templateId)
  }

  def cleanup(role: Role)(implicit API: engine.API): HiProgram[Unit] = {
    import API.client.users.deleteRole
    for {
      _ <- deleteUsers(role.principals.map(_.username).toList)
      _ <- deleteRole(role.rolename)
    } yield ()
  }

  def simpleScenario(templateId: Template.ID, user: User)(implicit API: client.API): Program[Unit] = {
    import API.{releases, tasks, users, log}

    def msg(s: String) = log.info(s"${user.username}: $s")

    for {
      _ <- msg(s"logging in as ${user.username}...")
      userSession <- users.login(user)
      _ <- msg(s"Creating release from template for user ${user.username}")
      releaseId <- releases.create(userSession, templateId, CreateReleaseArgs(
        title = s"${user.username}'s test dsl",
        variables = Map("var1" -> "Happy!"))
      )
      taskIds <- releases.getTasksByTitle(userSession, releaseId, "UI")
      taskId = taskIds.head
      _ <- msg(s"Assigning task ${taskId.show} to ${user.username}")
      _ <- tasks.assignTo(userSession, taskId, user.username)
      _ <- msg(s"Starting release $releaseId")
      _ <- releases.start(userSession, releaseId)
      _ <- msg(s"Waiting for task ${taskId.show}")
      _ <- tasks.waitFor(userSession, taskId, TaskStatus.InProgress, retries = Some(10))
      _ <- msg(s"Completing task ${taskId.show}")
      _ <- tasks.complete(userSession, taskId)
      _ <- msg("Waiting for release to complete")
      _ <- releases.waitFor(userSession, releaseId, ReleaseStatus.Completed)
    } yield ()
  }

  def fullScenario(template: Template, numUsers: Int)(implicit API: engine.API): HiProgram[Unit] = {
    import API.engine.parallel
    for {
      params <- setup(template, numUsers)
      (role, templateId) = params
      users = role.principals.toList
      _ <- parallel(numUsers) {
        n: Int => simpleScenario(templateId, users(n))
      }
      _ <- cleanup(role)
    } yield ()
  }


  def generateUsers(n: Int): List[User] = (0 to n).toList.map(i => User(s"user$i", "", "", s"user$i"))

  def createUsers(n: Int)(implicit API: engine.API): HiProgram[List[User]] = {
    import API.client.users.createUser

    generateUsers(n).map(u =>
      createUser(u).map(_ => u)
    ).sequence
  }

  def deleteUsers(users: List[User.ID])(implicit API: engine.API): HiProgram[Unit] = {
    import API.client.users.deleteUser

    users.map(deleteUser).sequence.map(_ => ())
  }

  def createGlobalRole(rolename: Role.ID)(users: List[User])(implicit API: engine.API): HiProgram[Role] = {
    import API.client.users.createRole

    val role = Role(rolename, Set(CreateTemplate, CreateRelease, CreateTopLevelFolder), users.toSet)

    createRole(role).map(_ => role)
  }
}

package com.xebialabs.xlrelease.stress

import com.xebialabs.xlrelease.stress.api.xlr.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.domain.Permission.{CreateRelease, CreateTemplate, CreateTopLevelFolder}
import com.xebialabs.xlrelease.stress.domain._
import cats._
import cats.implicits._
import cats.syntax._
import com.xebialabs.xlrelease.stress.domain.Member.RoleMember
import com.xebialabs.xlrelease.stress.domain.Team.{releaseAdmin, templateOwner}
import com.xebialabs.xlrelease.stress.api.{API, Program}
import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.nondeterminism._


object TestScenarios {

  def setup(template: Template, numUsers: Int)(implicit api: API): Program[(Role, Template.ID)] = {
    for {
      admin       <- api.xlr.users.admin()
      role        <- createUsers(numUsers) >>= createGlobalRole("superDuperRole")
      members     = Seq(RoleMember(role.rolename))
      templateId  <- api.xlr.releases.importTemplate(admin, template)
      _           <- api.xlr.releases.setTemplateScriptUser(admin, templateId, scriptUser = Some(admin.user))
      _           <- api.xlr.releases.setTemplateTeams(admin, templateId, Seq(templateOwner(members), releaseAdmin(members)))
    } yield (role, templateId)
  }

  def cleanup(role: Role)(implicit api: API): Program[Unit] = {
    for {
      _ <- deleteUsers(role.principals.map(_.username).toList)
      _ <- api.xlr.users.deleteRole(role.rolename)
    } yield ()
  }

  def simpleScenario(templateId: Template.ID, user: User)(implicit api: API): Program[Unit] = {
    def msg(s: String): api.log.FS[Unit] = api.log.info(s"${user.username}: $s")

    for {
      _ <- msg(s"logging in as ${user.username}...")
      userSession <- api.xlr.users.login(user)
      _ <- msg(s"Creating release from template for user ${user.username}")
      releaseId <- api.xlr.releases.create(userSession, templateId, CreateReleaseArgs(
        title = s"${user.username}'s test dsl",
        variables = Map("var1" -> "Happy!"))
      )
      taskIds <- api.xlr.releases.getTasksByTitle(userSession, releaseId, "UI")
      taskId = taskIds.head
      _ <- msg(s"Assigning task ${taskId.show} to ${user.username}")
      _ <- api.xlr.tasks.assignTo(userSession, taskId, user.username)
      _ <- msg(s"Starting release $releaseId")
      _ <- api.xlr.releases.start(userSession, releaseId)
      _ <- msg(s"Waiting for task ${taskId.show}")
      _ <- api.xlr.tasks.waitFor(userSession, taskId, TaskStatus.InProgress, retries = Some(10))
      _ <- msg(s"Completing task ${taskId.show}")
      _ <- api.xlr.tasks.complete(userSession, taskId)
      _ <- msg("Waiting for release to complete")
      _ <- api.xlr.releases.waitFor(userSession, releaseId, ReleaseStatus.Completed)
    } yield ()
  }

  def fullScenario(template: Template, numUsers: Int)(implicit api: API): Program[Unit] = {
    for {
      params <- setup(template, numUsers)
      (role, templateId) = params
      users = role.principals.toList
      _ <- api.control.parallel(numUsers) {
        n: Int => simpleScenario(templateId, users(n))
      }
      _ <- cleanup(role)
    } yield ()
  }

  def generateUsers(n: Int): List[User] = (0 to n).toList.map(i => User(s"user$i", "", "", s"user$i"))

  def createUsers(n: Int)(implicit api: API): Program[List[User]] = {
    generateUsers(n).map(u =>
      api.xlr.users.createUser(u).map(_ => u)
    ).sequence
  }

  def deleteUsers(users: List[User.ID])(implicit api: API): Program[Unit] = {
    users.map(api.xlr.users.deleteUser).sequence.map(_ => ())
  }

  def createGlobalRole(rolename: Role.ID)(users: List[User])(implicit api: API): Program[Role] = {

    val role = Role(rolename, Set(CreateTemplate, CreateRelease, CreateTopLevelFolder), users.toSet)

    api.xlr.users.createRole(role).map(_ => role)
  }
}

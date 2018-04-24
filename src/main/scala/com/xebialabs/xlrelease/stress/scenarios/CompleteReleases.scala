package com.xebialabs.xlrelease.stress.scenarios

import cats.Show
import com.xebialabs.xlrelease.stress.dsl.xlr.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.domain.Permission.{CreateRelease, CreateTemplate, CreateTopLevelFolder}
import com.xebialabs.xlrelease.stress.domain._
import cats.implicits._
import com.xebialabs.xlrelease.stress.domain.Member.RoleMember
import com.xebialabs.xlrelease.stress.domain.Team.{releaseAdmin, templateOwner}
import com.xebialabs.xlrelease.stress.dsl.{API, Program}
import com.xebialabs.xlrelease.stress.utils.TmpResource
import freestyle.free._
import freestyle.free.implicits._

import scala.io.Source

case class CompleteReleases(numUsers: Int) extends Scenario[(Role, Template.ID)] with ScenarioUtils[(Role, Template.ID)] {
  override val name: String = s"Simple scenario ($numUsers users)"

  val dsl_1mb: Template = Template("Simple Template", TmpResource("DSL_1mb.xlr"))

  val template = Source.fromResource("DSL-template.groovy")
    .getLines()
    .mkString("\n")

  override def setup: Program[(Role, Template.ID)] =
    api.xlr.users.admin() flatMap { implicit session =>
      for {
        role        <- createUsers(numUsers) >>= createGlobalRole("superDuperRole")
        members     = Seq(RoleMember(role.rolename))
        templateId  <- createReleaseFromGroovy(
          "Title of the Release that will Create Template from groovy",
          template,
          "###ADMIN_PASSWORD###",
          session.user.password
        )
//        _ <- api.log.info("template created: "+ templateId)
        _ <- api.xlr.releases.setTemplateTeams(templateId, Seq(templateOwner(members), releaseAdmin(members)))
//        _ <- api.log.info("template teams setup correctly")
      } yield (role, templateId)
    }

  override def program(params: (Role, Template.ID)): Program[Unit] = params match {
    case (role, templateId) =>
      val users = role.principals.toList
      api.control.parallel(numUsers) { n =>
        simple(templateId, users(n))
      }.map(_ => ())
  }

  protected def simple(templateId: Template.ID, user: User): Program[Unit] = {
    def msg(s: String): api.log.FS[Unit] =
      api.log.info(s"${user.username}: $s")

    api.xlr.users.login(user) flatMap { implicit session =>
      for {
        _ <- msg(s"logged in as ${user.username}...")
        _ <- msg("Creating release from template")
        releaseId <- api.xlr.releases.createFromTemplate(templateId, CreateReleaseArgs(
          title = s"${user.username}'s test dsl",
          variables = Map("var1" -> "Happy!")
        ))
        taskIds <- api.xlr.releases.getTasksByTitle(releaseId, "UI")
        taskId = taskIds.head
        _ <- msg(s"Assigning task ${taskId.show} to ${user.username}")
        _ <- api.xlr.tasks.assignTo(taskId, user.username)
        _ <- msg(s"Starting release $releaseId")
        _ <- api.xlr.releases.start(releaseId)
        _ <- msg(s"Waiting for task ${taskId.show}")
        _ <- api.xlr.tasks.waitFor(taskId, TaskStatus.InProgress, retries = None)
        _ <- msg(s"Completing task ${taskId.show}")
        _ <- api.xlr.tasks.complete(taskId)
        _ <- msg("Waiting for release to complete")
        _ <- api.xlr.releases.waitFor(releaseId, ReleaseStatus.Completed, retries = None)
      } yield ()
    }
  }

  override def cleanup(params: (Role, Template.ID)): Program[Unit] = params match {
    case (role, _) =>
      for {
        _ <- api.log.info("Cleaning up users and role")
        _ <- deleteUsers(role.principals.map(_.username).toList)
        _ <- api.xlr.users.deleteRole(role.rolename)
      } yield ()
  }

  implicit val showParams: Show[(Role, Template.ID)] = {
    case (role, templateId) =>
      s"role"
  }

  protected def createGlobalRole(rolename: Role.ID)(users: List[User]): Program[Role] = {
    val role = Role(rolename, Set(CreateTemplate, CreateRelease, CreateTopLevelFolder), users.toSet)

    api.log.info(s"Creating global role for ${users.size} users...").flatMap { _ =>
      api.xlr.users.createRole(role).map(_ => role)
    }
  }

  protected def generateUsers(n: Int): List[User] =
    (0 to n).toList.map(i => User(s"user$i", "", "", s"user$i"))

  protected def createUsers(n: Int): Program[List[User]] = {
    (api.log.info(s"Creating $n users..."): Program[Unit]) >> {
      generateUsers(n).map(u =>
        api.xlr.users.createUser(u).map(_ => u)
      ).sequence: Program[List[User]]
    }
  }

  protected def deleteUsers(users: List[User.ID]): Program[Unit] = {
    users.map(api.xlr.users.deleteUser).sequence.map(_ => ())
  }

}

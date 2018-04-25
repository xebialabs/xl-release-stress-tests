package com.xebialabs.xlrelease.stress.scenarios

import cats.Show
import com.xebialabs.xlrelease.stress.dsl.libs.xlr.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.domain.Permission.{CreateRelease, CreateTemplate, CreateTopLevelFolder}
import com.xebialabs.xlrelease.stress.domain._
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrConfig, XlrServer}
import com.xebialabs.xlrelease.stress.domain.Member.RoleMember
import com.xebialabs.xlrelease.stress.dsl.DSL
import com.xebialabs.xlrelease.stress.utils.TmpResource
import freestyle.free._
import freestyle.free.implicits._

import scala.io.Source

case class CompleteReleases(numUsers: Int)
                           (implicit
                            val config: XlrConfig,
                            val _api: DSL[DSL.Op])
  extends Scenario[(Role, Template.ID)]
    with ScenarioUtils {

  val name: String = s"Simple scenario ($numUsers users)"

  val template: String = Source.fromResource("DSL-template.groovy")
    .getLines()
    .mkString("\n")

  override def setup: Program[(Role, Template.ID)] =
    api.xlr.users.admin() flatMap { implicit session =>
      for {
        role        <- createUsers(numUsers) >>= createGlobalRole("superDuperRole")
        members     = Seq(RoleMember(role.roleName))
        templateId  <- createReleaseFromGroovy(
          "Create Mater Performance Template from groovy",
          template,
          "###ADMIN_PASSWORD###",
          session.user.password
        )
        _ <- api.log.info("template created: "+ templateId)
        _ <- api.log.info("getting template teams...")
        teams <- api.xlr.templates.getTeams(templateId)
        teamsMap = teams.map(t => t.teamName -> t).toMap
        templateOwner = teamsMap("Template Owner") match {
          case team => team.copy(members = team.members :+ RoleMember(role.roleName))
        }
        releaseAdmin = teamsMap("Release Admin") match {
          case team => team.copy(members = team.members :+ RoleMember(role.roleName))
        }
        _ <- api.log.info("setting up teams:")
        _ <- api.log.info(s"template owner: ${templateOwner.show}")
        _ <- api.log.info(s"release admin: ${releaseAdmin.show}")
        _ <- api.xlr.templates.setTeams(templateId, Seq(templateOwner, releaseAdmin))
        _ <- api.log.info("template teams setup correctly")
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
    api.xlr.users.login(user) flatMap { implicit session =>
      for {
        _ <- api.log.info(s"logged in as ${user.username}...")
        _ <- api.log.info("Creating release from template")
        releaseId <- api.xlr.releases.createFromTemplate(templateId, CreateReleaseArgs(
          title = s"${user.username}'s test dsl",
          variables = Map("var1" -> "Happy!")
        ))
        taskIds <- api.xlr.releases.getTasksByTitle(releaseId, "UI")
        taskId = taskIds.head
        _ <- api.log.info(s"Assigning task ${taskId.show} to ${user.username}")
        _ <- api.xlr.tasks.assignTo(taskId, user.username)
        _ <- api.log.info(s"Starting release $releaseId")
        _ <- api.xlr.releases.start(releaseId)
        _ <- api.log.info(s"Waiting for task ${taskId.show}")
        _ <- api.xlr.tasks.waitFor(taskId, TaskStatus.InProgress, retries = None)
        _ <- api.log.info(s"Completing task ${taskId.show}")
        _ <- api.xlr.tasks.complete(taskId)
        _ <- api.log.info("Waiting for release to complete")
        _ <- api.xlr.releases.waitFor(releaseId, ReleaseStatus.Completed, retries = None)
      } yield ()
    }
  }

  override def cleanup(params: (Role, Template.ID)): Program[Unit] = params match {
    case (role, _) =>
      for {
        _ <- api.log.info("Cleaning up users and role")
        _ <- deleteUsers(role.principals.map(_.username).toList)
        _ <- api.xlr.users.deleteRole(role.roleName)
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
    (0 until n).toList.map(i => User(s"user$i", "", "", s"user$i"))

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

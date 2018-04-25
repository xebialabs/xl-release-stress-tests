package com.xebialabs.xlrelease.stress.scenarios

import cats.Show
import com.xebialabs.xlrelease.stress.dsl.xlr.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.domain.Permission.{CreateRelease, CreateTemplate, CreateTopLevelFolder}
import com.xebialabs.xlrelease.stress.domain._
import cats.implicits._
import com.xebialabs.xlrelease.stress.domain.Member.RoleMember
import com.xebialabs.xlrelease.stress.dsl.Program
import com.xebialabs.xlrelease.stress.utils.TmpResource
import freestyle.free._
import freestyle.free.implicits._

import scala.io.Source

case class CompleteReleases(numUsers: Int) extends Scenario[(Role, Template.ID)] with ScenarioUtils {
  override val name: String = s"Simple scenario ($numUsers users)"

  val masterAutomationTpl = Source.fromResource("MasterAutomationTemplate.groovy")
    .getLines()
    .mkString("\n")

  val masterSeedTpl = Source.fromResource("MasterSeedTemplate.groovy")
    .getLines()
    .mkString("\n")

  override def setup: Program[(Role, Template.ID)] =
    api.xlr.users.admin() flatMap { implicit session =>
      for {
        role        <- createUsers(numUsers) >>= createGlobalRole("superDuperRole")
        members     = Seq(RoleMember(role.roleName))
        automationId  <- createReleaseFromGroovy(
          "Create Master Automation Template from groovy",
          masterAutomationTpl,
          "###ADMIN_PASSWORD###",
          session.user.password
        )
        seedId <- createReleaseFromGroovy(
          "Create Master Seed Template from groovy",
          masterSeedTpl,
          "###ADMIN_PASSWORD###",
          session.user.password
        )
        _ <- setupTeams(role, automationId)
        _ <- setupTeams(role, seedId)
      } yield (role, seedId)
    }

  private def setupTeams(role: Role, templateId: Template.ID)(implicit session: User.Session): Program[Unit] =
    for {
      _ <- api.log.info("template created: " + templateId)
      _ <- api.log.info("getting template teams...")
      teams <- api.xlr.releases.getTemplateTeams(templateId)
      teamsMap = teams.map(t => t.teamName -> t).toMap
      templateOwner = teamsMap.get("Template Owner").get match {
        case team => team.copy(members = team.members :+ RoleMember(role.roleName))
      }
      releaseAdmin = teamsMap.get("Release Admin").get match {
        case team => team.copy(members = team.members :+ RoleMember(role.roleName))
      }
      _ <- api.log.info("setting up teams:")
      _ <- api.log.info(templateOwner.show)
      _ <- api.log.info(releaseAdmin.show)
      _ <- api.xlr.releases.setTemplateTeams(templateId, Seq(templateOwner, releaseAdmin))
      _ <- api.log.info("template teams setup correctly")
    } yield ()

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
        seedRelId <- api.xlr.releases.createFromTemplate(templateId, CreateReleaseArgs(
          title = s"Seed release by ${user.username}",
          variables = Map("user" -> user.username)
        ))
        _ <- msg(s"Starting release $seedRelId")
        _ <- api.xlr.releases.start(seedRelId)
        taskIds <- api.xlr.releases.getTasksByTitle(seedRelId, "CR1")
        taskId = taskIds.head
        _ <- msg(s"Waiting for task ${taskId.show}")
        _ <- api.xlr.tasks.waitFor(taskId, TaskStatus.Completed, retries = None)
        _ <- api.log.debug(s"getComments(${taskId.show})")
        comments <- api.xlr.tasks.getComments(taskId)
        _ <- api.log.debug(s"comments: ${comments.mkString("\n")}")
        comment <- comments.init.lastOption.map(api.control.ok).getOrElse {
          api.control.fail(s"No comments in task ${taskId.show}")
        }
        _ <- api.log.debug("last comment: "+ comment)
        createdRelId <- getIdFromComment(comment.text).map(api.control.ok).getOrElse {
          api.control.fail(s"Cannot extract releaseId from comment: ${comment}")
        }
        manualTaskIds <- api.xlr.releases.getTasksByTitle(createdRelId, "UI")
        uiTaskId = manualTaskIds.head
        - <- api.xlr.tasks.assignTo(uiTaskId, user.username)
        _ <- msg(s"Completing task ${uiTaskId.show}")
        _ <- api.xlr.tasks.complete(uiTaskId)
        _ <- msg("Waiting for release to complete")
        _ <- api.xlr.releases.waitFor(seedRelId, ReleaseStatus.Completed, retries = None)
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

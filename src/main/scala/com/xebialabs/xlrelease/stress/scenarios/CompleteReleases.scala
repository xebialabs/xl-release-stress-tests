package com.xebialabs.xlrelease.stress.scenarios

import cats.Show
import com.xebialabs.xlrelease.stress.dsl.libs.xlr.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.domain.Permission.{CreateRelease, CreateTemplate, CreateTopLevelFolder}
import com.xebialabs.xlrelease.stress.domain._
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.XlrConfig
import com.xebialabs.xlrelease.stress.domain.Member.RoleMember
import com.xebialabs.xlrelease.stress.dsl.DSL
import freestyle.free._
import freestyle.free.implicits._
import org.joda.time.DateTime

import scala.concurrent.duration._
import scala.io.Source

case class CompleteReleases(numUsers: Int)
                           (implicit
                            val config: XlrConfig,
                            val _api: DSL[DSL.Op])
  extends Scenario[(Role, Template.ID)]
    with ScenarioUtils {

  val name: String = s"Complete ${numUsers} Releases "

  val masterAutomationTpl: String = Source.fromResource("MasterAutomationTemplate.groovy")
    .getLines()
    .mkString("\n")

  val masterSeedTpl: String = Source.fromResource("MasterSeedTemplate.groovy")
    .getLines()
    .mkString("\n")

  override def setup: Program[(Role, Template.ID)] =
    api.xlr.users.admin() flatMap { implicit session =>
      for {
        role        <- createUsers(numUsers) >>= createGlobalRole("superDuperRole")
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
      teams <- api.xlr.templates.getTeams(templateId)
      teamsMap = teams.map(t => t.teamName -> t).toMap
      templateOwner = teamsMap("Template Owner") match {
        case team => team.copy(members = team.members :+ RoleMember(role.roleName))
      }
      releaseAdmin = teamsMap("Release Admin") match {
        case team => team.copy(members = team.members :+ RoleMember(role.roleName))
      }
      _ <- api.log.info("setting up teams:")
      _ <- api.log.info(templateOwner.show)
      _ <- api.log.info(releaseAdmin.show)
      _ <- api.xlr.templates.setTeams(templateId, Seq(templateOwner, releaseAdmin))
      _ <- api.log.info("template teams setup correctly")
    } yield ()

  override def program(params: (Role, Template.ID)): Program[Unit] = params match {
    case (role, templateId) =>
      val users = role.principals.toList
      val generateLoad = rampUp(1, 32, _ * 2) { n =>
        simple(templateId, users(n))
      }.map(_ => ())
      for {
        results <- withHealthCheck[Unit, Int](program = generateLoad, checkInterval = new FiniteDuration(1, SECONDS), checkProgram = ???)
        (_, health) = results
        // TODO: show time serie
      } yield ()
  }

  protected def withHealthCheck[A, B](program: Program[A], checkInterval: FiniteDuration, checkProgram: Program[B]): Program[(A, List[(DateTime, FiniteDuration, B)])] =
    api.control.backgroundOf(program) {
      for {
        res <- healthCheck(checkProgram)
        _ <- api.control.sleep(checkInterval)
      } yield res
    }

  protected def healthCheck[A](program: Program[A]): Program[(DateTime, FiniteDuration, A)] =
    for {
      start <- api.control.now()
      result <- api.control.time(program)
    } yield (start, result._1, result._2)

  // TODO: move to control lib
  def rampUp[A](start: Int, end: Int, step: Int => Int)(program: Int => Program[A]): Program[List[List[A]]] = {
    RampUpRange.toList(RampUpRange(start, end, step)).map { n =>
      api.control.parallel[A](n) { i =>
        program(i)
      }
    }.sequence
  }

  case class RampUpRange(start: Int, end: Int, step: Int => Int = _ + 1)

  object RampUpRange {
    def toStream(range: RampUpRange): Stream[Int] =
      if (range.start <= range.end) {
        range.start #:: toStream(range.copy(start = range.step(range.start)))
      } else Stream.empty

    def toList(range: RampUpRange): List[Int] = toStream(range).toList
  }

  protected def simple(templateId: Template.ID, user: User): Program[Unit] = {
    api.xlr.users.login(user) flatMap { implicit session =>
      for {
        _ <- api.log.info(s"logged in as ${user.username}...")
        _ <- api.log.info("Creating release from template")
        seedRelId <- api.xlr.releases.createFromTemplate(templateId, CreateReleaseArgs(
          title = s"Seed release by ${user.username}",
          variables = Map("user" -> user.username)
        ))
        _ <- api.log.info(s"Starting release $seedRelId")
        _ <- api.xlr.releases.start(seedRelId)
        taskIds <- api.xlr.releases.getTasksByTitle(seedRelId, "CR1")
        taskId = taskIds.head
        _ <- api.log.info(s"Waiting for task ${taskId.show}")
        _ <- api.xlr.tasks.waitFor(taskId, TaskStatus.Completed, retries = None)
        _ <- api.log.debug(s"getComments(${taskId.show})")
        comments <- api.xlr.tasks.getComments(taskId)
        _ <- api.log.debug(s"comments: ${comments.mkString("\n")}")
        comment <- comments.init.lastOption.map(c => api.ok[Comment](c)).getOrElse {
          api.fail(s"No comments in task ${taskId.show}")
        }
        _ <- api.log.debug("last comment: "+ comment)
        createdRelId <- getIdFromComment(comment.text).map(id => api.ok[Release.ID](id)).getOrElse {
          api.fail(s"Cannot extract releaseId from comment: ${comment}")
        }
        manualTaskIds <- api.xlr.releases.getTasksByTitle(createdRelId, "UI")
        uiTaskId = manualTaskIds.head
        - <- api.xlr.tasks.assignTo(uiTaskId, user.username)
        _ <- api.log.info(s"Completing task ${uiTaskId.show}")
        _ <- api.xlr.tasks.complete(uiTaskId)
        _ <- api.log.info("Waiting for release to complete")
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

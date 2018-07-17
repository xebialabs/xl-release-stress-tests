package com.xebialabs.xlrelease.stress.scenarios

import java.util.concurrent.TimeUnit

import cats.Show
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.XlrConfig
import com.xebialabs.xlrelease.stress.domain.Permission.{CreateRelease, CreateTemplate, CreateTopLevelFolder}
import com.xebialabs.xlrelease.stress.domain.ReleaseStatus._
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.dsl.DSL
import freestyle.free._
import freestyle.free.implicits._

import scala.concurrent.duration._
import scala.language.postfixOps

case class Generate(numReleases: Int,
                    finalStatus: ReleaseStatus)
                   (implicit
                     val config: XlrConfig,
                     val _api: DSL[DSL.Op])
  extends Scenario[Role]
    with ScenarioUtils {

  val name: String = s"FailRetry $numReleases Releases"

  val suffixSize: Int = Math.max(1, Math.log10(numReleases).ceil.toInt)

  override def setup: Program[Role] =
    api.xlr.users.admin() flatMap { implicit session =>
      createUsers(numReleases) >>= createGlobalRole("users")
    }

  override def program(params: Role): Program[Unit] = programReal(params)

  def programReal(params: Role): Program[Unit] = {
    val role = params
    val users = role.principals.toList.sortBy(_.username)

    for {
      _ <- api.log.info(s"${this.getClass.getSimpleName}(${users.length}, $finalStatus)")
      _ <- grouped(howMany = numReleases, groupSize = Math.min(10, Math.max(1, numReleases / 2)))(n => singleUserScenario(users(n)))
    } yield ()
  }

  override def cleanup(params: Role): Program[Unit] = {
    val role = params
    for {
      _ <- api.log.info("Cleaning up users...")
      _ <- deleteUsers(role.principals.map(_.username).toList.sorted)
      _ <- api.log.info("Cleaning up role...")
      _ <- api.xlr.users.deleteRole(role.roleName)
    } yield ()
  }

  implicit val showParams: Show[Role] = Role.showRole(Permission.showPermission, User.showUser)

  protected def planned(): Program[Unit] =
    ().pure[Program]

  protected def inProgress(t1: Task.ID, t2: Task.ID)
                          (implicit session: User.Session): Program[Unit] = {
    val releaseId = t1.phaseId.release
    for {
      _ <- api.xlr.releases.start(releaseId)
      _ <- failAndRetry(t1, 1)
      _ <- api.xlr.tasks.complete(t1, Some("completed"))
      _ <- failAndRetry(t2, 1)
    } yield ()
  }

  protected def completed(t1: Task.ID, t2: Task.ID)
                         (implicit session: User.Session): Program[Unit] =
    for {
      _ <- inProgress(t1, t2)
      _ <- api.xlr.tasks.complete(t2, Some("completed"))
    } yield ()

  protected def failed(t1: Task.ID, t2: Task.ID)
                      (implicit session: User.Session): Program[Unit] =
    for {
      _ <- inProgress(t1, t2)
      _ <- api.xlr.tasks.fail(t2, "Failed")
    } yield ()

  protected def aborted(t1: Task.ID, t2: Task.ID)
                       (implicit session: User.Session): Program[Unit] =
    for {
      _ <- failed(t1, t2)
      _ <- api.xlr.releases.abort(t2.release)
    } yield ()

  protected def singleUserScenario(user: User): Program[Unit] =
    api.xlr.users.login(user) flatMap { implicit session =>
      for {
        tasks <- createRelease()
        (t1, t2) = tasks
        _ <- finalStatus match {
          case Planned => planned()
          case InProgress => inProgress(t1, t2)
          case Failed => failed(t1, t2)
          case Completed => completed(t1, t2)
          case Aborted => aborted(t1, t2)
          case _ =>
            ().pure[Program]
        }
      } yield ()
    }

  protected def createRelease()
                             (implicit session: User.Session): Program[(Task.ID, Task.ID)] =
    for {
      phaseId <- api.xlr.releases.createRelease(s"Release ${session.user.username}", Some(session.user))
      t1 <- api.xlr.phases.appendTask(phaseId, "t1", "xlrelease.Task")
      _ <- api.xlr.tasks.assignTo(t1, session.user.username)
      t2 <- api.xlr.phases.appendTask(phaseId, "t2", "xlrelease.Task")
      _ <- api.xlr.tasks.assignTo(t2, session.user.username)
    } yield (t1, t2)

  protected def failAndRetry(taskId: Task.ID, times: Int)
                            (implicit session: User.Session): Program[List[Unit]] =
    (1 to times).toList.map { i =>
      for {
        _ <- api.xlr.tasks.fail(taskId, s"failed#$i")
        _ <- api.xlr.tasks.retry(taskId, s"retry$i")
      } yield ()
    }.sequence


  protected def createGlobalRole(rolename: Role.ID)(users: List[User]): Program[Role] = {
    val role = Role(rolename, Set(CreateTemplate, CreateRelease, CreateTopLevelFolder), users.toSet)

    for {
      _ <- api.log.info(s"Creating global role for ${users.size} users...")
      persisted <- api.xlr.users.createRole(role).map(_ => role)
    } yield persisted
  }

  protected def generateUsers(n: Int): List[User] =
    (1 to n).toList.map { i =>
      val username = s"user%0${suffixSize}d".format(i)
      User(username, "", "", username)
    }

  protected def createUsers(n: Int): Program[List[User]] =
    for {
      _ <- api.log.info(s"Creating $n users...")
      users <- generateUsers(n).map(u => api.xlr.users.createUser(u).map(_ => u)).sequence
    } yield users

  protected def deleteUsers(users: List[User.ID]): Program[Unit] =
    users.map(api.xlr.users.deleteUser).sequence
      .map(_ => ())

}

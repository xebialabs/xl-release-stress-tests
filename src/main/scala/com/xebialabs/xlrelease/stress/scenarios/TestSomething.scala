package com.xebialabs.xlrelease.stress.scenarios

import cats.Show
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.XlrConfig
import com.xebialabs.xlrelease.stress.dsl.DSL
import com.xebialabs.xlrelease.stress.domain.{Task, TaskStatus, Template, User}
import com.xebialabs.xlrelease.stress.dsl.libs.xlr.protocol.CreateReleaseArgs
import freestyle.free._
import freestyle.free.implicits._

import scala.concurrent.duration._
import scala.language.postfixOps


case class TestSomething(templateId: Template.ID)
                        (implicit
                         val config: XlrConfig,
                         val _api: DSL[DSL.Op])
  extends Scenario[Unit]
    with ScenarioUtils {

  val name: String = s"Test Something"

  override def setup: Program[Unit] = ().pure[Program]

  override def program(params: Unit): Program[Unit] =
    api.xlr.users.admin() >>= { implicit session =>
      (1 to 1000).toList.map { n =>
        for {
          releaseId <- api.xlr.releases.createFromTemplate(templateId, CreateReleaseArgs(title = s"Release #$n", variables = Map("v1" -> s"test#$n")))
          _ <- api.xlr.releases.start(releaseId)
          t3Id <- api.xlr.releases.getTasksByTitle(releaseId, "t3").map(_.head)
          _ <- api.xlr.tasks.waitFor(t3Id, TaskStatus.InProgress, 2 seconds, None)
          _ <- api.log.info(s"failAndRetry($t3Id, 4)")
          _ <- failAndRetry(t3Id, 4)
        } yield ()
      }.sequence[Program, Unit].map(_ => ())
    }

  override def cleanup(params: Unit): Program[Unit] = ().pure[Program]

  override implicit val showParams: Show[Unit] = _ => ""

  protected def failAndRetry(taskId: Task.ID, times: Int)
                            (implicit session: User.Session): Program[List[Unit]] =
    (1 to times).toList.map { i =>
      for {
        _ <- api.xlr.tasks.fail(taskId, s"failed#$i")
        _ <- api.xlr.tasks.retry(taskId, s"retry$i")
      } yield ()
    }.sequence
}

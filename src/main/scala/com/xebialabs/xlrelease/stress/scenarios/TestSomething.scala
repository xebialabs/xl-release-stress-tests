package com.xebialabs.xlrelease.stress.scenarios

import cats.Show
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.XlrConfig
import com.xebialabs.xlrelease.stress.dsl.DSL
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.dsl.libs.xlr.protocol.CreateReleaseArgs
import freestyle.free._
import freestyle.free.implicits._

import scala.concurrent.duration._
import scala.language.postfixOps


case class TestSomething(templateId: String, howMany: Int)
                        (implicit
                         val config: XlrConfig,
                         val _api: DSL[DSL.Op])
  extends Scenario[Unit]
    with ScenarioUtils {

  val name: String = s"Test Something"

  override def setup: Program[Unit] = ().pure[Program]

  override def program(params: Unit): Program[Unit] =
    api.xlr.users.admin() >>= { implicit session =>
      rampUp(8, howMany, _ * 2) { _ =>
        api.control.repeat(5) {
          for {
            releaseId <- api.xlr.releases.createFromTemplate(templateId, CreateReleaseArgs("Test Release", Map.empty))
            _ <- api.log.info(s"[${releaseId.show}] created")
            t1 <- api.xlr.releases.getTasksByTitle(releaseId, "t1").map(_.head)
            t2 <- api.xlr.releases.getTasksByTitle(releaseId, "t2").map(_.head)
            _ <- api.xlr.releases.start(releaseId)
            start <- api.control.now()
            _ <- api.log.info(s"[${releaseId.show}] started")
            _ <- api.xlr.tasks.fail(t1, "Fail 1")
            _ <- api.xlr.tasks.retry(t1, "Retry 1")
            _ <- api.xlr.tasks.fail(t1, "Fail 2")
            _ <- api.xlr.tasks.retry(t1, "Retry 2")
            _ <- api.xlr.tasks.fail(t1, "Fail 3")
            _ <- api.xlr.tasks.retry(t1, "Retry 3")
            _ <- api.xlr.tasks.fail(t1, "Fail 4")
            _ <- api.xlr.tasks.retry(t1, "Retry 4")
            _ <- api.xlr.tasks.complete(t1, Some("Complete t1"))
            _ <- api.xlr.tasks.waitFor(t2, TaskStatus.InProgress, 1 second, None)
            end <- api.control.now()
            _ <- api.log.info(s"[${releaseId.show}] done in ${end.getMillis - start.getMillis}ms")
          } yield ()
        }.map(_ => ())
      }.map(_ => ())
    }

  override def cleanup(params: Unit): Program[Unit] = ().pure[Program]

  override implicit val showParams: Show[Unit] = _ => ""
}

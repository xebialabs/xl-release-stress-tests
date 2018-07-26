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


case class TestSomething(templateId: Template.ID,
                         howMany: Int)
                        (implicit
                         val config: XlrConfig,
                         val _api: DSL[DSL.Op])
  extends Scenario[Unit]
    with ScenarioUtils {

  val name: String = s"Test Something"

  override def setup: Program[Unit] = ().pure[Program]

  override def program(params: Unit): Program[Unit] =
    api.xlr.users.admin() >>= { implicit session =>
      rampUp(2, howMany, _ * 2) { _ =>
        for {
          releaseId <- api.xlr.releases.createFromTemplate(templateId, CreateReleaseArgs(title = "Whatever", variables = Map.empty))
          _ <- api.log.info(s"[${releaseId.show}] created")
          _ <- api.xlr.releases.start(releaseId)
          start <- api.control.now()
          _ <- api.log.info(s"[${releaseId.show}] started")
          _ <- api.xlr.releases.waitFor(releaseId, ReleaseStatus.Completed, 5 seconds, None)
          end <- api.control.now()
          _ <- api.log.info(s"[${releaseId.show}] completed in ${start.getMillis - end.getMillis}ms")
        } yield ()
      }.map(_ => ())
    }

  override def cleanup(params: Unit): Program[Unit] = ().pure[Program]

  override implicit val showParams: Show[Unit] = _ => ""
}

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
      api.control.repeat(1) {
        api.control.parallel(howMany) { i =>
          for {
            releaseId <- api.xlr.releases.createFromTemplate(templateId, CreateReleaseArgs(title = "Whatever", variables = Map.empty))
            _ <- api.xlr.releases.start(releaseId)
            _ <- api.xlr.releases.waitFor(releaseId, ReleaseStatus.Completed, 5 seconds, None)
          } yield ()
        }.map(_ => ())
      }.map(_ => ())
    }

  override def cleanup(params: Unit): Program[Unit] = ().pure[Program]

  override implicit val showParams: Show[Unit] = _ => ""
}

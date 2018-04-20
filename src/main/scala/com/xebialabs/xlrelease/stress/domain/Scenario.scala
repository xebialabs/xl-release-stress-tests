package com.xebialabs.xlrelease.stress.domain

import cats.effect.IO
import cats.implicits._
import com.xebialabs.xlrelease.stress.Runner
import com.xebialabs.xlrelease.stress.api.{API, Program}
import com.xebialabs.xlrelease.stress.handlers.xlr.akkaClient.AkkaHttpXlrClient
import freestyle.free._
import freestyle.free.implicits._

import scala.concurrent.ExecutionContext

case class Scenario[A](name: String, program: Program[A])

object Scenario {
  implicit class ScenarioOps[A](val scenario: Scenario[A]) extends AnyVal {
    def runIO(implicit client: AkkaHttpXlrClient, api: API, ec: ExecutionContext): IO[A] =
      Runner.runIO[A] {
        for {
          _ <- api.log.info(s"Starting scenario ${scenario.name}")
          a <- scenario.program
          _ <- api.log.info(s"Scenario ${scenario.name} completed")
        } yield a
      }
  }

}

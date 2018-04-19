package com.xebialabs.xlrelease.stress.domain

import cats.effect.IO
import cats.implicits._
import com.xebialabs.xlrelease.stress.client.{Program, XLRClient}
import com.xebialabs.xlrelease.stress.client.akkaClient.{AkkaHttpXlrClient, Runner}
import freestyle.free._
import freestyle.free.implicits._

case class Scenario[A](name: String, program: Program[A])

object Scenario {
  implicit class ScenarioOps[A](val scenario: Scenario[A]) extends AnyVal {
    def runIO(implicit client: AkkaHttpXlrClient, API: XLRClient[XLRClient.Op]): IO[A] =
      Runner.runIO[A] {
        for {
          _ <- API.log.info(s"Starting scenario ${scenario.name}")
          a <- scenario.program
          _ <- API.log.info(s"Scenario ${scenario.name} completed")
        } yield a
      }
  }

}

package com.xebialabs.xlrelease.stress.scenarios

import cats.Show
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.dsl
import com.xebialabs.xlrelease.stress.runners.io

import scala.concurrent.ExecutionContext


trait Scenario[A] extends dsl.API {
  def name: String

  def setup: dsl.Program[A]

  def program(params: A): dsl.Program[Unit]

  def cleanup(params: A): dsl.Program[Unit]

  implicit val showParams: Show[A]
}

object Scenario {
  implicit class ScenarioOps[A](val scenario: Scenario[A]) extends AnyVal {
    def run(implicit ec: ExecutionContext): Unit = {
      implicit val server: XlrServer = scenario.config.server
      implicit val password: AdminPassword = scenario.config.adminPassword
      implicit val ctx: io.RunnerContext = io.runnerContext
      io.runScenario(scenario)
    }
  }
}

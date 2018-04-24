package com.xebialabs.xlrelease.stress.scenarios

import cats.Show
import com.xebialabs.xlrelease.stress.dsl.{API, Program}
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.runners.io
import com.xebialabs.xlrelease.stress.utils.AkkaHttpClient

import scala.concurrent.ExecutionContext

trait Scenario[A] {
  val api: API = implicitly[API]

  def name: String

  def setup: Program[A]

  def program(params: A): Program[Unit]

  def cleanup(params: A): Program[Unit]

  implicit val showParams: Show[A]
}

object Scenario {
  implicit class ScenarioOps[A](val scenario: Scenario[A]) extends AnyVal {
    def run(implicit
            server: XlrServer,
            admin: AdminPassword,
            client: AkkaHttpClient,
            ec: ExecutionContext): Unit = {
      implicit val ctx: io.RunnerContext = io.runnerContext
      io.runScenario(scenario)
    }
  }
}

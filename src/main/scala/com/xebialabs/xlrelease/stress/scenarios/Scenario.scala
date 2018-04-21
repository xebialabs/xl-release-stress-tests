package com.xebialabs.xlrelease.stress.scenarios

import cats.Show
import com.xebialabs.xlrelease.stress.dsl
import com.xebialabs.xlrelease.stress.dsl.{API, Program}
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.dsl.handlers.io
import com.xebialabs.xlrelease.stress.http.handlers.future.AkkaHttpClient

import scala.concurrent.ExecutionContext

trait Scenario[A] {
  val api: API = implicitly[API]

  def name: String

  def setup: Program[A]

  def program(params: A): Program[Unit]

  def cleanup(params: A): Program[Unit] = dsl.nop

  implicit val showParams: Show[A]
}

object Scenario {
  implicit class ScenarioOps[A](val scenario: Scenario[A]) extends AnyVal {
    def run(implicit
            server: XlrServer,
            admin: AdminPassword,
            client: AkkaHttpClient,
            ec: ExecutionContext): Unit = {
      io.runScenario(scenario)
    }
  }
}

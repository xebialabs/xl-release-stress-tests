package com.xebialabs.xlrelease.stress.scenarios

import com.xebialabs.xlrelease.stress.api.{API, Program}
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.handlers.io
import com.xebialabs.xlrelease.stress.http.AkkaHttpClient

import scala.concurrent.ExecutionContext

trait Scenario {
  val api: API = implicitly[API]

  def name: String

  def program: Program[Unit]

}

object Scenario {
  case class Simple(name: String, program: Program[Unit])(override implicit val api: API) extends Scenario

  def apply(name: String, program: Program[Unit])
           (implicit api: API): Scenario = Simple(name, program)(api)

  implicit class ScenarioOps(val scenario: Scenario) extends AnyVal {
    def run(implicit
            server: XlrServer,
            admin: AdminPassword,
            client: AkkaHttpClient,
            ec: ExecutionContext): Unit =
      io.runScenario(scenario)
  }
}

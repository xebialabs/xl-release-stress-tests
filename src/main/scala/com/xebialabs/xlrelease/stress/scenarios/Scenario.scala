package com.xebialabs.xlrelease.stress.scenarios

import com.xebialabs.xlrelease.stress.Runner
import com.xebialabs.xlrelease.stress.api.{API, Program}
import com.xebialabs.xlrelease.stress.handlers.xlr.akkaClient.AkkaHttpXlrClient

import scala.concurrent.ExecutionContext

trait Scenario {
  lazy val api: API = implicitly[API]

  def name: String

  def program: Program[Unit]

}

object Scenario {
  case class Simple(name: String, program: Program[Unit]) extends Scenario
  def apply(name: String, program: Program[Unit]): Scenario = Simple(name, program)


  implicit class ScenarioOps(val scenario: Scenario) extends AnyVal {
    def run(implicit client: AkkaHttpXlrClient, ec: ExecutionContext): Unit =
      Runner.instance.runScenario(scenario)
  }
}

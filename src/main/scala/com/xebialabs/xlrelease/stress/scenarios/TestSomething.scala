package com.xebialabs.xlrelease.stress.scenarios

import cats.Show
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.XlrConfig
import com.xebialabs.xlrelease.stress.domain.Template.ID
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.dsl.DSL
import freestyle.free.implicits._

import scala.io.Source

case class TestSomething()
                        (implicit
                         val config: XlrConfig,
                         val _api: DSL[DSL.Op])
  extends Scenario[(Role, Template.ID)]
    with ScenarioUtils {

  val name: String = s"Test Something"

  override def setup: Program[(Role, Template.ID)] = ???

  override def program(params: (Role, Template.ID)): Program[Unit] = ???

  override def cleanup(params: (Role, Template.ID)): Program[Unit] = ???

  override implicit val showParams: Show[(Role, ID)] = ???
}

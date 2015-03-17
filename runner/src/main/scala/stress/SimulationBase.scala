package stress

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder

import scala.concurrent.duration._
import scala.language.postfixOps

abstract class SimulationBase(scenarios: List[ScenarioBuilder]) extends Simulation {

  def this(scenario: ScenarioBuilder) = this(List(scenario))

  setUp(
    scenarios.map(if (!devMode) {
      _.inject(
        atOnceUsers(1),
        nothingFor(20 seconds),
        rampUsers(nbUsers) over (15 seconds)
      )
    } else {
      _.inject(
        rampUsers(nbUsers) over (2 seconds)
      )
//      .customPauses("1 second")
    })
  ).protocols(httpProtocol)

}

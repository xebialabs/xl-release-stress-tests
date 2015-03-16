package stress

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder

import scala.concurrent.duration._
import scala.language.postfixOps

abstract class SimulationBase(scenarios: List[ScenarioBuilder]) extends Simulation {

  def this(scenario: ScenarioBuilder) = this(List(scenario))

  setUp(
    scenarios.map(scenario => {
      val populatedScenario = scenario.inject(
        atOnceUsers(1),
        nothingFor(20 seconds),
        rampUsers(nbUsers) over (15 seconds)
      )
      if (devMode) {
        populatedScenario.customPauses("1 second")
      } else {
        populatedScenario
      }
    })
  ).protocols(httpProtocol)

}

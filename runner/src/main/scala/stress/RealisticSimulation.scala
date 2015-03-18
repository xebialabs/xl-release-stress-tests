package stress

import io.gatling.core.scenario.Simulation
import stress.utils.Scenarios._
import io.gatling.core.Predef._

/**
 * A simulation which combines several roles of people working with XL Release
 * in one realistic usage scenario.
 */
class RealisticSimulation extends Simulation {

  setUp(
    releaseManagerScenario.inject(rampUsers(nbReleaseManagers) over rampUpPeriod),
    opsScenario.inject(rampUsers(nbOps) over rampUpPeriod),
    developmentTeamScenario.inject(rampUsers(nbTeams) over rampUpPeriod)
  ).protocols(httpProtocol)

}

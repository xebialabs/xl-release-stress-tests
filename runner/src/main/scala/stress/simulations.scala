package stress

import io.gatling.core.Predef._
import stress.chain.Releases
import stress.config.RunnerConfig
import stress.utils.Scenarios._

import scala.language.{implicitConversions, postfixOps}

/**
 * X users open calendar page on a given month
 */
class CalendarSimulation extends SimulationBase(openCalendarScenario)

/**
 * X concurrent users create releases (without template and tasks) simultaneously and get task definitions.
 */
class CreateReleaseSimulation extends SimulationBase(createReleaseScenario)

/**
 * X concurrent users are opening tasks overview page
 */
class TasksOverviewSimulation extends SimulationBase(queryAllTasksScenario)

/**
 * X concurrent users are opening tasks overview page with a text filter which won't ever match
 */
class TasksOverviewWithFilterSimulation extends SimulationBase(queryNonExistingTaskScenario)

/**
 * X users poll status of 300 tasks
 */
class PollingSimulation extends SimulationBase(pollingScenario)

/**
 * X users are looking at release overview
 */
class ReleasesOverviewSimulation extends SimulationBase(
  scenario("Release overview").exec(Releases.queryAllActive)
)

/**
 * X users are looking at templates overview without filters
 */
class TemplateOverviewSimulation extends SimulationBase(queryTemplatesScenario)

/**
 * X users are looking at the details of the release at Release Flow screen
 */
class ReleaseFlowSimulation extends SimulationBase(releaseFlowScenario)

/**
 * X users open Pipeline page
 */
class PipelineSimulation extends SimulationBase(queryPipelinesScenario)


/**
 * A simulation which combines several roles of people working with XL Release
 * in one realistic usage scenario.
 */
class RealisticSimulation extends Simulation {

  val rampUpPeriod = RunnerConfig.simulations.realistic.rampUpPeriod
  val repeats = RunnerConfig.simulations.realistic.repeats

  setUp(
    releaseManagerScenario(repeats).inject(rampUsers(RunnerConfig.input.releaseManagers) over rampUpPeriod),
    opsScenario(repeats).inject(rampUsers(RunnerConfig.input.ops) over rampUpPeriod),
    developmentTeamScenario(repeats).inject(rampUsers(RunnerConfig.input.teams) over rampUpPeriod)
  ).protocols(httpProtocol)
}

/**
 * X release managers are working with XL Release
 */
class ReleaseManagerSimulation extends SimulationBase(releaseManagerScenario(1))

/**
 * X ops people are working with XL Release
 */
class OpsSimulation extends SimulationBase(opsScenario(1))

/**
 * X development teams commit code which triggers new releases. Each teams consists of ~10 developers.
 */
class DevelopmentTeamSimulation extends SimulationBase(developmentTeamScenario(1))


/**
  * X release managers open folders and their releases and templates.
  */
class FoldersSimulation extends Simulation {
  val rampUpPeriod = RunnerConfig.simulations.realistic.rampUpPeriod
  val repeats = RunnerConfig.simulations.realistic.repeats

  setUp(
    folderScenario(repeats).inject(rampUsers(RunnerConfig.input.releaseManagers) over rampUpPeriod)
  ).protocols(httpProtocol)

}

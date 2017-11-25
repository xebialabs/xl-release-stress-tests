package stress

import io.gatling.core.Predef._
import stress.chain.Releases
import stress.config.RunnerConfig
import stress.config.RunnerConfig.simulations
import stress.utils.Scenarios.{riskCRUDScenario, _}

import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}

/**
  * X users open Risks settings page,
  * Y riskManagers edit Risk profiles,
  * 1 riskManager edits 1 risk profile linked to 500 releases,
  *  X riskManager, 1 admin create and delete risk profile
  */
class RealisticRiskSimulation extends Simulation{
  private val rampUpPeriod = simulations.realistic.rampUpPeriod
  private val repeats = simulations.realistic.repeats

  setUp(
    openRisksScenario(repeats).inject(rampUsers(RunnerConfig.input.ops) over rampUpPeriod)
      .protocols(httpProtocolNormalUser),
    editRiskScenario(repeats).inject(rampUsers(RunnerConfig.input.ops) over rampUpPeriod)
      .protocols(httpProtocolRiskUser),
    riskManagerScenario.inject(atOnceUsers(RunnerConfig.input.ciso))
      .protocols(httpProtocol),
    riskCRUDScenario(repeats)
      .inject(rampUsers(RunnerConfig.input.releaseManagers) over rampUpPeriod)
      .protocols(httpProtocol)
  )
}

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
  * in one realistic usage scenario. Compatible with XLR version before 6.0.0, when
  * there were no folders yet.
  */
class RealisticSimulation500 extends Simulation {

  private val rampUpPeriod = simulations.realistic.rampUpPeriod
  private val repeats = simulations.realistic.repeats

  setUp(
    releaseManagerScenario(repeats).inject(rampUsers(RunnerConfig.input.releaseManagers) over rampUpPeriod),
    opsScenario(repeats).inject(rampUsers(RunnerConfig.input.ops) over rampUpPeriod),
    developmentTeamScenario500(repeats).inject(rampUsers(RunnerConfig.input.teams) over rampUpPeriod)
  ).protocols(httpProtocol)
}


/**
  * A realistic scenario for XLR 6.0.0+: several roles of people working with XL Release, including
  * opening folders.
  */
class RealisticSimulation extends Simulation {

  private val rampUpPeriod = simulations.realistic.rampUpPeriod
  private val repeats = simulations.realistic.repeats

  setUp(
    releaseManagerScenario(repeats).inject(rampUsers(RunnerConfig.input.releaseManagers) over rampUpPeriod),
    opsScenario(repeats).inject(rampUsers(RunnerConfig.input.ops) over rampUpPeriod),
    opsBulkScenario(repeats).inject(rampUsers(RunnerConfig.input.opsBulk) over rampUpPeriod),
    developmentTeamScenario(repeats).inject(rampUsers(RunnerConfig.input.teams) over rampUpPeriod)
  ).protocols(httpProtocol)
}

/**
  * A simulation which combines several roles of people working with XL Release
  * in one realistic usage scenario.
  */
class SequentialSimulation extends Simulation {

  setUp(
    sequentialScenario(1).inject(rampUsers(RunnerConfig.input.users) over (1 seconds))
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
  * X ops people are working with bulk operations in XL Release
  */
class OpsBulkSimulation extends SimulationBase(opsBulkScenario(1))

/**
  * X development teams commit code which triggers new releases. Each teams consists of ~10 developers.
  */
class DevelopmentTeamSimulation extends SimulationBase(developmentTeamScenario(1))

/**
  * X users interact with dependencies endpoints
  */
class DependenciesSimulation extends SimulationBase(dependenciesScenario(simulations.repeats))

/**
  * X release managers open folders and their releases and templates.
  */
class FoldersSimulation extends Simulation {
  setUp(
    folderScenario(simulations.realistic.repeats)
      .inject(rampUsers(RunnerConfig.input.releaseManagers) over simulations.realistic.rampUpPeriod)
  ).protocols(httpProtocol)
}

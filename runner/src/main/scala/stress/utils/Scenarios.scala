package stress.utils

import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef._
import io.gatling.http.request.StringBody
import stress.chain._
import stress.config.RunnerConfig._

import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}

object Scenarios {

  val createReleaseScenario: ScenarioBuilder = scenario("Create release")
    .exec(Releases.create(RawFileBody("create-release-body.json")))

  val queryAllReleasesScenario: ScenarioBuilder = scenario("Query all releases")
    .exec(Releases.queryAllActive)

  val queryPipelinesScenario: ScenarioBuilder = scenario("Query pipelines")
    .exec(Pipeline.query(StringBody("""{"onlyMine":false,"onlyFlagged":false,"filter":"","active":true}""")))

  val openCalendarScenario: ScenarioBuilder = scenario("Calendar page")
    .exec(Calendar.open)

  val queryAllTasksScenario: ScenarioBuilder = scenario("Query all tasks")
    .exec(Tasks.open("Get list of all tasks", StringBody(Tasks.ALL_TASKS_FILTER)))

  val queryNonExistingTaskScenario: ScenarioBuilder = scenario("Query all tasks with non-existing filter")
    .exec(Tasks.open("Get list of non-existing tasks", StringBody(Tasks.NOT_EXISTING_TASKS_FILTER)))

  val pollingScenario: ScenarioBuilder = scenario("Poll 320 tasks")
    .exec(Tasks.pollManyTasks)

  val releaseFlowScenario: ScenarioBuilder = scenario("Release flow")
    .exec(Releases.flow("Release1"))

  val queryTemplatesScenario: ScenarioBuilder = scenario("Template overview")
    .exec(Templates.open)

  def releaseManagerChain500(releaseManagerPauseMin: Duration, releaseManagerPauseMax: Duration): ChainBuilder = {
    exec(Pipeline.query(StringBody( """{"onlyMine":false,"onlyFlagged":false,"filter":"","active":true}""")))
      .pause(releaseManagerPauseMin, releaseManagerPauseMax)
      .exec(Calendar.open)
  }

  def releaseManagerChain(opsPauseMin: FiniteDuration, opsPauseMax: Duration): ChainBuilder = {
    exec(Folders.open)  // Open folders
      .exec(Folders.openFolderTemplates)  // List templates in that folder
      .pause(opsPauseMin, opsPauseMax)  // wait
      .exec(Folders.openFolderReleases)      // list planned releases in that folder
      // start two releases from that folder, if there are any planned releases
      .pause(opsPauseMin, opsPauseMax)      // wait
      .exec(Releases.queryAllActive)  // Open releases overview - active releases
      // Abort two random releases, if any
      .pause(opsPauseMin, opsPauseMax) // wait
      // Open releases overview - planned releases
      // Open a random release
      // wait
      .exec(Releases.queryAllCompleted) // open releases overview - completed release
      .pause(opsPauseMin, opsPauseMax)  // wait
      .exec(Templates.open) // open templates overview
      .pause(opsPauseMin, opsPauseMax)  // wait
      .exec(Calendar.open)  // open calendar
  }

  def opsChain(opsPauseMin: FiniteDuration, opsPauseMax: Duration, taskPollDuration: Duration, taskPollPause: Duration): ChainBuilder = {
    exec(Tasks.openAndPoll("Get list of my tasks", Tasks.MY_TASKS_FILTER, taskPollDuration, taskPollPause))
      .exec(Calendar.open)
      .pause(opsPauseMin, opsPauseMax)
      .exec(Tasks.commentOnRandomTask())
      .pause(opsPauseMin, opsPauseMax)
      .exec(Tasks.changeTeamAssignmentOfRandomTask())
      .pause(opsPauseMin, opsPauseMax)
      .exec(Tasks.openAndPoll("Get list of my tasks", Tasks.MY_TASKS_FILTER, taskPollDuration / 1.7, taskPollPause))
  }

  def developmentTeamChain(devPause: Duration): ChainBuilder = {
    exec(Releases.createFromTemplate("create-release-many-automated-tasks.json", "Many automated tasks"))
      .pause(devPause)
      .exec(PublicApi.getTemplates)
      .pause(devPause)
  }

  def dependenciesChain(pauseMin: Duration, pauseMax: Duration): ChainBuilder = {
    exec(Releases.getRandomTreeRelease)
      .pause(pauseMin, pauseMax)
      .exec(Releases.getRelease)
      .pause(pauseMin, pauseMax)
      .exec(Releases.getDependencies)
      .pause(pauseMin, pauseMax)
      .exec(Releases.getDependencyTree)
      .pause(pauseMin, pauseMax)
      .exec(Tasks.getDependencyCandidates)
  }

  def releaseManagerScenario(repeats: Int): ScenarioBuilder = {
    scenario("Release manager")
      .repeat(repeats)(
        releaseManagerChain(opsPauseMin, opsPauseMax)
      )
  }

  def opsScenario(repeats: Int): ScenarioBuilder = {
    scenario("Ops person")
      .repeat(repeats)(
        opsChain(opsPauseMin, opsPauseMax, taskPollDuration, taskPollPause)
      )
  }

  def opsBulkScenario(repeats: Int): ScenarioBuilder = {
    scenario("Ops person (bulk operations)")
      .repeat(repeats)(
        exec(Releases.getRandomTreeRelease)
          .pause(opsBulkPauseMin, opsBulkPauseMax)
          .exec(Releases.getRelease)
          .pause(opsBulkPauseMin, opsBulkPauseMax)
          .exec(Releases.getReleasePlannedTaskIds)
          .exec(Tasks.commentOnTasks)
          .pause(opsBulkPauseMin, opsBulkPauseMax)
          .exec(Tasks.changeAssignmentOnTasks)
          .pause(opsBulkPauseMin, opsBulkPauseMax)
          .exec(Tasks.removeTasks)
          .pause(opsBulkPauseMin, opsBulkPauseMax)
      )
  }

  def opsBulkReleaseScenario(): ScenarioBuilder = {
    /*
    Depending on how many releases there are, bulk start and abort them 50/50
     */
    scenario("Ops person (bulk start/abort releases)")
      .exec(Releases.queryMutable())
  }

  def developmentTeamScenario500(repeats: Int): ScenarioBuilder = scenario("Team of developers")
    .repeat(repeats)(
      repeat(2) {
        exec(Releases.createFromTemplate("create-release-many-automated-tasks.json", "Many automated tasks"))
          .pause(devPause)
      }
    )

  def developmentTeamScenario(repeats: Int): ScenarioBuilder = scenario("Team of developers")
    .repeat(repeats)(
      developmentTeamChain(devPause)
    )

  def folderScenario(repeats: Int): ScenarioBuilder = scenario("Folder scenario")
    .repeat(repeats)(
      exec(Folders.open)
        .exec(Folders.openFolderTemplates)
        .pause(opsPauseMin, opsPauseMax)
        .exec(Folders.openFolderReleases)
        .pause(opsPauseMin, opsPauseMax)
        .exec(Releases.queryAllActive)
        .pause(opsPauseMin, opsPauseMax)
        .exec(Releases.queryAllCompleted)
        .exec(Templates.open)
    )

  def dependenciesScenario(repeats: Int): ScenarioBuilder = {
    scenario("Dependencies scenario")
      .repeat(repeats)(
        dependenciesChain(userPauseMin, userPauseMax)
      )
  }

  def sequentialScenario(repeats: Int): ScenarioBuilder = scenario("Person fulfilling all roles")
    .repeat(repeats)(
      exec(
        releaseManagerChain500(1 second, 1 second),
        developmentTeamChain(1 second),
        opsChain(1 second, 1 second, 1 second, 1 second)
      )
    )

}

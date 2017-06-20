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
    exec(Folders.open)
      .exec(Folders.openFolderTemplates)
      .pause(opsPauseMin, opsPauseMax)
      .exec(Folders.openFolderReleases)
      .pause(opsPauseMin, opsPauseMax)
      .exec(Releases.queryAllActive)
      .pause(opsPauseMin, opsPauseMax)
      .exec(Releases.queryAllCompleted)
      .pause(opsPauseMin, opsPauseMax)
      .exec(Templates.open)
      .pause(opsPauseMin, opsPauseMax)
      .exec(Calendar.open)
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

  def bulkOpsScenario(repeats: Int): ScenarioBuilder = {
    scenario("(Bulk) Ops person")
      .repeat(repeats)(
        exec(Releases.getRandomTreeRelease)
          .pause(bulkOpsPauseMin, bulkOpsPauseMax)
          .exec(Releases.getRelease)
          .pause(bulkOpsPauseMin, bulkOpsPauseMax)
          .exec(Releases.getReleaseTaskIds)
          .exec(Tasks.commentOnTasks)
          .pause(bulkOpsPauseMin, bulkOpsPauseMax)
          .exec(Tasks.changeAssignmentOnTasks)
          .pause(bulkOpsPauseMin, bulkOpsPauseMax)
          .exec(Tasks.removeTasks)
          .pause(bulkOpsPauseMin, bulkOpsPauseMax)
      )
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

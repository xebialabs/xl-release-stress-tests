package stress.utils

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.StringBody
import stress.chain._
import stress.config.RunnerConfig._

import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}

object Scenarios {

  val createReleaseScenario = scenario("Create release")
    .exec(Releases.create(RawFileBody("create-release-body.json")))

  val queryAllReleasesScenario = scenario("Query all releases")
    .exec(Releases.queryAllActive)

  val queryPipelinesScenario = scenario("Query pipelines")
    .exec(Pipeline.query(StringBody("""{"onlyMine":false,"onlyFlagged":false,"filter":"","active":true}""")))

  val openCalendarScenario = scenario("Calendar page")
    .exec(Calendar.open)

  val queryAllTasksScenario = scenario("Query all tasks")
    .exec(Tasks.open("Get list of all tasks", StringBody(Tasks.ALL_TASKS_FILTER)))

  val queryNonExistingTaskScenario = scenario("Query all tasks with non-existing filter")
    .exec(Tasks.open("Get list of non-existing tasks", StringBody(Tasks.NOT_EXISTING_TASKS_FILTER)))

  val pollingScenario = scenario("Poll 320 tasks")
    .exec(Tasks.pollManyTasks)

  val releaseFlowScenario = scenario("Release flow")
    .exec(Releases.flow("Release1"))

  val queryTemplatesScenario = scenario("Template overview")
    .exec(Templates.open)

  def releaseManagerChain(releaseManagerPauseMin: Duration, releaseManagerPauseMax: Duration) = {
    exec(Pipeline.query(StringBody( """{"onlyMine":false,"onlyFlagged":false,"filter":"","active":true}""")))
      .pause(releaseManagerPauseMin, releaseManagerPauseMax)
      .exec(Calendar.open)
  }

  def opsChain(opsPauseMin: FiniteDuration, opsPauseMax: Duration, taskPollDuration: Duration, taskPollPause: Duration) = {
    //exec(Tasks.openAndPoll("Get list of my tasks", Tasks.MY_TASKS_FILTER, taskPollDuration, taskPollPause))
      exec(Calendar.open)
      .pause(opsPauseMin, opsPauseMax)
      .exec(Tasks.commentOnRandomTask())
      .pause(opsPauseMin, opsPauseMax)
      .exec(Tasks.changeTeamAssignmentOfRandomTask())
      .pause(opsPauseMin, opsPauseMax)
      //.exec(Tasks.openAndPoll("Get list of my tasks", Tasks.MY_TASKS_FILTER, taskPollDuration / 1.7, taskPollPause))
  }

  def developmentTeamChain(devPause: Duration) = {
    exec(Releases.createFromTemplate("create-release-many-automated-tasks.json", "Many automated tasks"))
      .pause(devPause)
  }

  def dependenciesChain(pauseMin: Duration, pauseMax: Duration) = {
    exec(Releases.getRandomTreeRelease)
      .pause(pauseMin, pauseMax)
      .exec(Releases.getDependencies)
      .pause(pauseMin, pauseMax)
      .exec(Releases.getDependencyTree)
      .pause(pauseMin, pauseMax)
      .exec(Tasks.getDependencyCandidates)
  }

  def releaseManagerScenario(repeats: Int) = {
    scenario("Release manager")
      .repeat(repeats)(
        releaseManagerChain(releaseManagerPauseMin, releaseManagerPauseMax)
      )
  }

  def opsScenario(repeats: Int) = {
    scenario("Ops person")
      .repeat(repeats)(
        opsChain(opsPauseMin, opsPauseMax, taskPollDuration, taskPollPause)
      )
  }

  def developmentTeamScenario(repeats: Int) = scenario("Team of developers")
    .repeat(repeats)(
      repeat(2) {
        developmentTeamChain(devPause)
      }
    )

  def folderScenario(repeats: Int) = scenario("Folder scenario")
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

  def dependenciesScenario(repeats: Int) = {
    scenario("Dependencies scenario")
      .repeat(repeats)(
        dependenciesChain(userPauseMin, userPauseMax)
      )
  }

  def sequentialScenario(repeats: Int) = scenario("Person fulfilling all roles")
    .repeat(repeats)(
      exec(releaseManagerChain(1 seconds, 1 seconds), developmentTeamChain(1 seconds), opsChain(1 seconds, 1 seconds, 1 seconds, 1 seconds))
    )

}

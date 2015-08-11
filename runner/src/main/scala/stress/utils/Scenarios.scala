package stress.utils

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.StringBody
import stress.chain._
import stress.config.RunnerConfig._

import scala.language.{implicitConversions, postfixOps}

object Scenarios {

  val createReleaseScenario = scenario("Create release")
    .exec(Releases.create(RawFileBody("create-release-body.json")))

  val queryAllReleasesScenario = scenario("Query all releases")
    .exec(Releases.queryAll)

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

  def releaseManagerScenario(repeats: Int) = {
    scenario("Release manager")
      .repeat(repeats)(
        exec(Pipeline.query(StringBody( """{"onlyMine":false,"onlyFlagged":false,"filter":"","active":true}""")))
          .pause(releaseManagerPauseMin, releaseManagerPauseMax)
          .exec(Calendar.open)
      )
  }

  def opsScenario(repeats: Int) = {
    scenario("Ops person")
      .repeat(repeats)(
        exec(Tasks.openAndPoll("Get list of my tasks", Tasks.MY_TASKS_FILTER, taskPollDuration))
          .exec(Calendar.open)
          .pause(opsPauseMin, opsPauseMax)
          .exec(Tasks.commentOnRandomTask())
          .pause(opsPauseMin, opsPauseMax)
          .exec(Tasks.changeTeamAssignmentOfRandomTask())
          .pause(opsPauseMin, opsPauseMax)
          .exec(Tasks.openAndPoll("Get list of my tasks", Tasks.MY_TASKS_FILTER, taskPollDuration / 1.7))
      )
  }

  def developmentTeamScenario(repeats: Int) = scenario("Team of developers")
    .repeat(repeats)(
      repeat(2) {
        exec(Releases.createFromTemplate("create-release-many-automated-tasks.json", "Many automated tasks"))
        .pause(devPause)
      }
    )

}

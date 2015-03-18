package stress.utils

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.StringBody
import stress.chain._

import scala.concurrent.duration._
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

  val queryMyTasksScenario = scenario("Query my tasks")
    .exec(Tasks.open(RawFileBody("my-filters-body.json")))

  val releaseFlowScenario = scenario("Release flow")
    .exec(Releases.flow("Release1"))

  val queryTemplatesScenario = scenario("Template overview")
    .exec(Templates.open)

  val releaseManagerScenario = scenario("Release manager")
    .exec(Pipeline.query(StringBody("""{"onlyMine":false,"onlyFlagged":false,"filter":"","active":true}""")))
    .pause(4 minutes, 6 minutes)
    .exec(Calendar.open)
    .pause(4 minutes, 6 minutes)

  val opsScenario = scenario("Ops person")
    .exec(Tasks.openAndPoll(Tasks.MY_TASKS_FILTER, 4 minutes))
    .exec(Calendar.open)
    .pause(0.5 minute, 1.5 minutes)
    .exec(Tasks.commentOnRandomTask())
    .pause(0.5 minute, 1.5 minutes)
    .exec(Tasks.changeTeamAssignmentOfRandomTask())
    .pause(0.5 minute, 1.5 minutes)
    .exec(Tasks.openAndPoll(Tasks.MY_TASKS_FILTER, 3 minutes))

  val developmentTeamScenario = scenario("Team of developers")
    .repeat(2) {
      exec(Releases.createFromTemplate("create-release-20-automated-tasks.json", "20 automated tasks"))
      .pause(5 minutes)
    }

}

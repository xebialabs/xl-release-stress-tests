package stress.utils

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.StringBody
import stress.chain._

import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}

object Scenarios {

  val createReleaseScenario = scenario("Create release")
    .exec(Release.create(RawFileBody("create-release-body.json")))

  val queryAllReleasesScenario = scenario("Query all releases")
    .exec(Release.queryAll)

  val queryPipelinesScenario = scenario("Query pipelines")
    .exec(Pipeline.query(StringBody("""{"onlyMine":false,"onlyFlagged":false,"filter":"","active":true}""")))

  val openCalendarScenario = scenario("Calendar page")
    .exec(Calendar.open)

  val queryMyTasksScenario = scenario("Query my tasks")
    .exec(Tasks.open(RawFileBody("my-filters-body.json")))

  val releaseFlowScenario = scenario("Release flow")
    .exec(Release.flow("Release1"))

  val queryTemplatesScenario = scenario("Template overview")
    .exec(Template.query)

  val releaseManagerScenario = scenario("Release manager")
    .exec(Pipeline.query(StringBody("""{"onlyMine":false,"onlyFlagged":false,"filter":"","active":true}""")))
    .pause(4 minutes, 6 minutes)
    .exec(Calendar.open)
    .pause(4 minutes, 6 minutes)

  val opsScenario = scenario("Ops person")
    .exec(Tasks.openAndPoll(
      StringBody("""{"active":true,"assignedToMe":true,"assignedToMyTeams":false,"assignedToOthers":false,"notAssigned":false,"filter":""}"""),
      4 minutes
    ))
    .exec(Calendar.open)

}

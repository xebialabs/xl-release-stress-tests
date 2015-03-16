package stress.utils

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import stress.chain._

import scala.language.{implicitConversions, postfixOps}

object Scenarios {

  val createReleaseScenario = scenario("Create release")
    .exec(Release.create(RawFileBody("create-release-body.json")))

  val queryAllReleasesScenario = scenario("Query all releases")
    .exec(Release.queryAll)

  val queryPipelinesScenario = scenario("Query pipelines")
    .exec(Pipeline.query(StringBody( """{"onlyMine":false,"onlyFlagged":false,"filter":"","active":true}""")))

  val openCalendarScenario = scenario("Calendar page")
    .exec(Calendar.open)

  val queryMyTasksScenario = scenario("Query my tasks")
    .exec(Tasks.tasks(RawFileBody("my-filters-body.json")))

  val releaseFlowScenario = scenario("Release flow")
    .exec(Release.flow("Release1"))

  val queryTemplatesScenario = scenario("Template overview")
    .exec(Template.query)

}

package stress

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import stress.utils.TaskIds

import scala.language.postfixOps

/**
 * X users poll status of 100 tasks
 */
class PollingSimulation extends SimulationBase(
  scenario("Polling ...")
    .exec(
      http("Poll tasks")
        .post("/tasks/poll")
        .body(StringBody(
        s"""{"ids":[${
          val tt = TaskIds.generate(20, 4, 4).map(t => s""""$t"""").mkString(", ")
          tt
        }]}""")).asJSON
    )
)
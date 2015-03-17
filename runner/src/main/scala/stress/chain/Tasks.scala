package stress.chain

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.Body

import scala.concurrent.duration.{Duration, _}
import scala.language.postfixOps

object Tasks {

  def open(filter: Body) = exec(
    http("Get list of tasks")
      .post("/tasks/search?limitTasksHint=100")
      .body(filter)
      .asJSON
      .check(
        jsonPath("$['releaseTasks'][*]['tasks'][*]['id']")
          .findAll
          .saveAs("taskIds")
      )
  )

  def openAndPoll(filter: Body, pollDuration: Duration) = open(filter)
    .exitHereIfFailed
    .exec(session => {
      val ids: Vector[String] = session.attributes.get("taskIds").get.asInstanceOf[Vector[String]]
      session.set("pollTasksBody", s"""{"ids":[${ids.map(s => s""""$s"""").mkString(",")}]}""")
    })
    .during(pollDuration) {
      exec(
        http("Poll tasks")
          .post("/tasks/poll")
          .body(StringBody("${pollTasksBody}"))
          .asJSON
      )
      .pause(2 seconds)
    }

}

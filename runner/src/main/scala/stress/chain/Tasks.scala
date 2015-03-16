package stress.chain

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.Body

object Tasks {

  def tasks(filter: Body) = exec(
    http("Get list of tasks")
      .post("/tasks/search?limitTasksHint=100")
      .body(filter)
  )

}

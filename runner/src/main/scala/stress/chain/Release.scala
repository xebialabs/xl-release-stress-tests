package stress.chain
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.Body

object Release {

  def create(body: Body) =
    exec(http("Get templates").get("/releases/templates?depth=1"))
      .exec(http("Post release").post("/releases").body(body).asJSON)
      .exec(http("Get tasks-definitions").get("/tasks/task-definitions"))

  def queryAll = exec(
    http("All releases")
      .post("/releases/search")
      .queryParam("numberbypage", "15")
      .queryParam("page", "0")
      .body(RawFileBody("release-search-body.json")).asJSON
  )

  def flow(release: String) =
    exec(http("Get polling interval").get("/settings/polling-interval"))
      .exec(http("Get reports").get("/settings/reports"))
      .exec(http("Get security").get("/security"))
      .exec(http("Get release").get(s"/releases/$release"))
      .exec(http("Get tasks-definitions").get("/tasks/task-definitions"))



}

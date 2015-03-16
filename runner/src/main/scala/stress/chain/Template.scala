package stress.chain

import io.gatling.core.Predef._
import io.gatling.http.Predef._


object Template {

  def query = exec(
    http("All templates")
      .get("/releases/templates?filter=")
      .queryParam("numberbypage", "15")
      .queryParam("page", "0")
      .asJSON
  )
}

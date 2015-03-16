package stress.chain

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.Body

object Pipeline {

  def query(filter: Body) = exec(
        http("Get pipeline releases")
          .post("/releases/search")
          .body(filter)
          .queryParam("numberbypage", "15")
          .queryParam("page", "0")
          .asJSON
          .check(
            jsonPath("$['cis'][*]['id']")
              .findAll
              .saveAs("releaseIds")
          )
      ).execGetDependencies("pipeline")
}

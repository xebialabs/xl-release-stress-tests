package stress.chain

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.Body
import stress.config.RunnerConfig

object Pipeline {

  def query(filter: Body) = exec(
        http("Get pipeline releases")
          .post("/releases/search")
          .body(filter)
          .queryParam("numberbypage", RunnerConfig.queries.search.numberByPage)
          .queryParam("page", "0")
          .asJSON
          .check(
            jsonPath("$['cis'][*]['id']")
              .findAll
              .saveAs("releaseIds")
          )
      ).execGetDependencies("pipeline")
}

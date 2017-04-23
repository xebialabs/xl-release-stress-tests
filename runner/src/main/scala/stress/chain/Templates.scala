package stress.chain

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import stress.config.RunnerConfig


object Templates {

  def open: ChainBuilder = exec(
    http("All templates")
      .post("/releases/templates/search")
      .body(StringBody("""{"tags":[],"title":""}"""))
      .queryParam("numberbypage", RunnerConfig.queries.search.numberByPage)
      .queryParam("page", "0")
      .asJSON
  )

  def findTemplatesByTitle(title: String): ChainBuilder = exec(
    http("Find template by title")
      .post("/releases/templates/search")
      .body(StringBody(s"""{"tags":[],"title":"$title"}"""))
      .queryParam("numberbypage", RunnerConfig.queries.search.numberByPage)
      .queryParam("page", "0")
      .asJSON
      .check(
        jsonPath("$['cis'][*]['id']")
          .findAll
          .saveAs("foundTemplateIds")
      )
  )
}

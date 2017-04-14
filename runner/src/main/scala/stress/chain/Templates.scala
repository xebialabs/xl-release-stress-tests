package stress.chain

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import stress.config.RunnerConfig


object Templates {

  def open: ChainBuilder = exec(
    http("All templates")
      .get("/releases/templates?filter=")
      .queryParam("numberbypage", RunnerConfig.queries.search.numberByPage)
      .queryParam("page", "0")
      .asJSON
  )

  def findTemplatesByTitle(title: String): ChainBuilder = exec(
    http("Find template by title")
      .get(s"/releases/templates?filter=$title")
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

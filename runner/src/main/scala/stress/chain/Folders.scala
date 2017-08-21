package stress.chain

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.http.request.StringBody
import stress.config.RunnerConfig
import stress.filters.{ReleaseSearchFilter, TemplateSearchFilter}

object Folders {
  def open: ChainBuilder = exec(
    http("Open folders view")
      .get("/api/v1/folders/list")
      .queryParam("depth", 10)
      .queryParam("permissions", true)
      .asJSON
  )

  def openFolderReleasesPlanned: ChainBuilder =
    exec(
        http("Open folder planned releases")
          .post("/releases/search")
          .body(StringBody(ReleaseSearchFilter(planned = true, parentId = "Applications/Folder_1")))
          .queryParam("numberbypage", RunnerConfig.queries.search.numberByPage)
          .queryParam("page", "0")
          .asJSON
      )

  def openFolderTemplates: ChainBuilder = exec(
    http("Open folder templates")
      .post("/releases/templates/search")
      .body(StringBody(TemplateSearchFilter(
        parentId = "Applications/Folder_1"
      )))
      .queryParam("numberbypage", RunnerConfig.queries.search.numberByPage)
      .queryParam("page", "0")
      .asJSON
  )
}

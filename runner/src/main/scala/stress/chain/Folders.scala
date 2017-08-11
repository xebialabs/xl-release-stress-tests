package stress.chain

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.http.request.StringBody
import stress.config.RunnerConfig

object Folders {

  val RELEASES_FILTER = s"""{"active":false,"planned":true,"completed":false,"onlyMine":false,"onlyFlagged":false,"filter":"","parentId":"Applications/Folder_1"}"""

  def open: ChainBuilder = exec(
    http("Open folders view")
      .get("/api/v1/folders/list?depth=10&permissions=true")
      .asJSON
      .check(
        jsonPath("$[*]['id']")
          .findAll
          .saveAs("folderIds")
      )
  )

  def openFolderReleasesPlanned: ChainBuilder =
    exec(
        http("Open folder planned releases")
          .post("/releases/search")
          .body(StringBody(RELEASES_FILTER))
          .queryParam("numberbypage", RunnerConfig.queries.search.numberByPage)
          .queryParam("page", "0")
          .asJSON
      )

  def openFolderTemplates: ChainBuilder = exec(
    http("Open folder templates")
      .post("/releases/templates/search")
      .body(StringBody("""{"tags":[],"title":"","parentId":"Applications/Folder_1"}"""))
      .queryParam("numberbypage", RunnerConfig.queries.search.numberByPage)
      .queryParam("page", "0")
      .asJSON
  )

}

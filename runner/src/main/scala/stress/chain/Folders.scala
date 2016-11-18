package stress.chain

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.{Body, StringBody}
import stress.config.RunnerConfig

object Folders {

  def open = exec(
    http("Open all folders")
      .get("/api/v1/folders/list?depth=10&permissions=true")
      .asJSON
  )

  def openFolderReleases = exec(
    http("Open folder releases")
      .post("/releases/search")
      .body(StringBody("""{"active":true,"planned":true,"completed":false,"onlyMine":false,"onlyFlagged":false,"filter":"","parentId":"Applications/Folder_1"}"""))
      .queryParam("numberbypage", RunnerConfig.queries.search.numberByPage)
      .queryParam("page", "0")
      .asJSON
  )

  def openFolderTemplates = exec(
    http("Open folder templates")
      .get("/releases/templates")
      .queryParam("numberbypage", RunnerConfig.queries.search.numberByPage)
      .queryParam("page", "0")
      .queryParam("parentId", "Applications/Folder_1")
      .asJSON
  )


}

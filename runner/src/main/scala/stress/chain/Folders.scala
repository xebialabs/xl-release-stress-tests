package stress.chain

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.http.request.StringBody
import stress.config.RunnerConfig

object Folders {

  def releasesFilter(folder: String = "Applications/Folder_1") = {
    println("folder ID: ", folder)
    s"""{"active":false,"planned":true,"completed":false,"onlyMine":false,"onlyFlagged":false,"filter":"","parentId":"$folder"}"""
  }

  def open: ChainBuilder = exec(
    http("Open all folders, first level")
      .get("/api/v1/folders/list?depth=10&permissions=true")
      .asJSON
      .check(
        jsonPath("$[*]['id']")
          .findAll
          .saveAs("folderIds")
      )
  )

  def openChild: ChainBuilder = exec(
    http("Open all folders, second level")
      .get("/api/v1/folders/list?depth=10&permissions=true")
      .asJSON
      .check(
        jsonPath("$[${parentIdx}]['children'][*]['id']")
          //  .transformOption(ids =>)
          //.transformOption(ids => ids.orElse(Some("a")).saveAs("user_level"))
          .findAll
          .saveAs("childFolderIds")
      )
  )

  def openFolderReleasesPlanned: ChainBuilder =
    exec(
        http("Open folder planned releases")
          .post("/releases/search")
          .body(StringBody(releasesFilter("${folderId}")))
          .queryParam("numberbypage", RunnerConfig.queries.search.numberByPage)
          .queryParam("page", "0")
          .asJSON
          .check(
            jsonPath("$['cis'][*]['id']")
              .findAll
              .saveAs("folderReleasesPlanned")
          )
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

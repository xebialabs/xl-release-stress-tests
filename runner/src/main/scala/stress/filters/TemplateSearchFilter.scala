package stress.filters

import scala.util.parsing.json.JSONArray

object TemplateSearchFilter {
  def apply(tags: List[String] = List[String](),
            title: String = "",
            parentId: String = ""): String = {
    s"""{
       |"tags": ${JSONArray(tags)},
       |"title": "$title",
       |"parentId": "$parentId"
       |}""".stripMargin
  }
}

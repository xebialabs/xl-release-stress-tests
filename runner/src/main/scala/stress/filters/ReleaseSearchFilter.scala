package stress.filters

import java.util.Date

import scala.util.parsing.json.JSONArray

object ReleaseSearchFilter {
  def apply(active: Boolean = false,
            planned: Boolean = false,
            completed: Boolean = false,
            title: String = "",
            parentId: String = "",
            tags: List[String] = List[String](),
            from: Date = null,
            to: Date = null): String = {
    s"""{
       |"active": $active,
       |"planned": $planned,
       |"completed": $completed,
       |"title": "$title",
       |"parentId": "$parentId",
       |"tags": ${JSONArray(tags)},
       |"from": ${if (from == null) null else from.getTime},
       |"to": ${if (to == null) null else to.getTime}
       |}""".stripMargin
  }
}
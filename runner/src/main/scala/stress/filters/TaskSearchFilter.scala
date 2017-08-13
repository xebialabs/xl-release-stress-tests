package stress.filters

import java.util.Date

object TaskSearchFilter {
  def apply(active: Boolean = false,
            assignedToMe: Boolean = false,
            assignedToMyTeams: Boolean = false,
            assignedToOthers: Boolean = false,
            notAssigned: Boolean = false,
            filter: String = "",
            from: Date = null,
            to: Date = null): String = {
    s"""{
       |"active": $active,
       |"assignedToMe": $assignedToMe,
       |"assignedToMyTeams": $assignedToMyTeams,
       |"assignedToOthers": $assignedToOthers,
       |"notAssigned": $notAssigned,
       |"filter": "$filter",
       |"from": ${if (from == null) null else from.getTime},
       |"to": ${if (to == null) null else to.getTime}
       |}""".stripMargin
  }
}

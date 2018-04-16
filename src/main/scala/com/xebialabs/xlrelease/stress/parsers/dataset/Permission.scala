package com.xebialabs.xlrelease.stress.parsers.dataset

sealed abstract class Permission(val permission: String)

object Permission {
  case object Admin extends Permission("admin")
  case object EditSecurity extends Permission("security#edit")
  case object ViewReports extends Permission("reports#view")
  case object CreateTemplate extends Permission("template#create")
  case object CreateRelease extends Permission("release#create")
  case object EditGlobalVariables extends Permission("global_variables#edit")
  case object CreateTopLevelFolder extends Permission("folder#create_top_level")
  case object EditBlackout extends Permission("global_calendar#edit_blackout")
  case object EditRiskProfile extends Permission("risk_profile#edit")
}

package com.xebialabs.xlrelease.stress.parsers.dataset

import spray.json._

sealed abstract class Permission(val permission: String)

object Permission extends DefaultJsonProtocol {
  sealed trait Global { self: Permission => }
  sealed trait Local { self: Permission => }

  case object Admin extends Permission("admin") with Global
  case object EditSecurity extends Permission("security#edit") with Global
  case object ViewReports extends Permission("reports#view") with Global
  case object CreateTemplate extends Permission("template#create") with Global
  case object CreateRelease extends Permission("release#create") with Global
  case object EditGlobalVariables extends Permission("global_variables#edit") with Global
  case object CreateTopLevelFolder extends Permission("folder#create_top_level") with Global
  case object EditBlackout extends Permission("global_calendar#edit_blackout") with Global
  case object EditRiskProfile extends Permission("risk_profile#edit") with Global
  case object ViewTemplate extends Permission("template#view") with Local
  case object CreateReleaseFromTemplate extends Permission("template#create_release") with Local

  def allGlobalPermissions: Set[Permission with Global] = Set(Admin, EditSecurity, ViewReports, CreateTemplate, CreateRelease, EditGlobalVariables, CreateTopLevelFolder, EditBlackout, EditRiskProfile)
  def allLocalPermissions: Set[Permission with Local] = Set(CreateReleaseFromTemplate, ViewTemplate)

  implicit val permissionWriter: RootJsonWriter[Permission] = permission => permission.permission.toJson

  implicit val localPermissionWriter: RootJsonWriter[Permission with Local] = p => permissionWriter.write(p)
  implicit val globalPermissionWriter: RootJsonWriter[Permission with Global] = p => permissionWriter.write(p)

}

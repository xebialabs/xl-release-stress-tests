package com.xebialabs.xlrelease.stress.domain

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

  case object CreateReleaseFromTemplate extends Permission("template#create_release") with Local
  case object ViewTemplate extends Permission("template#view") with Local
  case object EditTemplate extends Permission("template#edit") with Local
  case object EditTemplateSecurity extends Permission("template#edit_security") with Local
  case object EditTriggers extends Permission("template#edit_triggers") with Local
  case object ViewRelease extends Permission("release#view") with Local
  case object EditRelease extends Permission("release#edit") with Local
  case object EditReleaseSecurity extends Permission("release#edit_security") with Local
  case object StartRelease extends Permission("release#start") with Local
  case object AbortRelease extends Permission("release#abort") with Local
  case object EditReleaseTask extends Permission("release#edit_task") with Local
  case object ReassignReleaseTask extends Permission("release#reassign_task") with Local
  case object EditTaskBlackout extends Permission("release#edit_blackout") with Local
  case object ViewFolder extends Permission("folder#view") with Local
  case object EditFolder extends Permission("folder#edit") with Local
  case object EditFolderSecurity extends Permission("folder#edit_security") with Local
  case object ViewReleaseGroup extends Permission("group#view") with Local
  case object EditReleaseGroup extends Permission("group#edit") with Local
  case object LockReleaseTask extends Permission("release#lock_task") with Local
  case object LockTemplateTask extends Permission("template#lock_task") with Local


  def allGlobalPermissions: Set[Permission with Global] = Set(
    Admin,
    EditSecurity,
    ViewReports,
    CreateTemplate,
    CreateRelease,
    EditGlobalVariables,
    CreateTopLevelFolder,
    EditBlackout,
    EditRiskProfile
  )

  def allLocalPermissions: Set[Permission with Local] = Set(
    CreateReleaseFromTemplate, ViewTemplate, EditTemplate, EditTemplateSecurity, EditTriggers,
    ViewRelease, EditRelease, EditReleaseSecurity, StartRelease, AbortRelease,
    EditReleaseTask, ReassignReleaseTask, EditTaskBlackout,
    ViewFolder, EditFolder, EditFolderSecurity,
    ViewReleaseGroup, EditReleaseGroup,
    LockReleaseTask, LockTemplateTask
  )

  implicit val permissionWriter: RootJsonWriter[Permission] = permission => permission.permission.toJson

  implicit val localPermissionWriter: RootJsonWriter[Permission with Local] = p => permissionWriter.write(p)
  implicit val globalPermissionWriter: RootJsonWriter[Permission with Global] = p => permissionWriter.write(p)

}

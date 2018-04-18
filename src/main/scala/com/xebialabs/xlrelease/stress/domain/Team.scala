package com.xebialabs.xlrelease.stress.domain

import spray.json._
import com.xebialabs.xlrelease.stress.domain.Permission._
import com.xebialabs.xlrelease.stress.domain.Member._

case class Team(teamName: String,
                members: Seq[Member],
                permissions: Seq[Permission with Permission.Local] = Seq(Permission.ViewTemplate),
                systemTeam: Boolean = false,
                id: Team.ID = None)

object Team extends DefaultJsonProtocol {
  type ID = Option[String]

  implicit val teamWriter: RootJsonWriter[Team] = team => JsObject(
    "id" -> team.id.toJson,
    "teamName" -> team.teamName.toJson,
    "members" -> team.members.map(Member.memberWriter.write).toJson,
    "permissions" -> team.permissions.map(Permission.permissionWriter.write).toJson,
    "systemTeam" -> team.systemTeam.toJson
  )

  val templateOwnerPermissions: Set[Permission with Permission.Local] = Set(
    CreateReleaseFromTemplate,
    ViewTemplate,
    EditTemplate,
    EditTemplateSecurity,
    EditTriggers,
    LockTemplateTask
  )

  val releaseAdminPermissions = Set(
    ViewTemplate,
    ViewRelease,
    EditRelease,
    EditReleaseSecurity,
    StartRelease,
    AbortRelease,
    EditReleaseTask,
    ReassignReleaseTask,
    EditTaskBlackout,
    LockReleaseTask
  )

  def templateOwner(members: Seq[Member]) =
    Team("Template Owner",
      members,
      templateOwnerPermissions.toSeq,
      systemTeam = true
    )

  def releaseAdmin(members: Seq[Member]) =
    Team("Release Admin",
      members,
      releaseAdminPermissions.toSeq,
      systemTeam = true
    )

}

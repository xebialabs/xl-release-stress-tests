package com.xebialabs.xlrelease.stress.domain

import cats._
import cats.implicits._
import com.xebialabs.xlrelease.stress.domain.Permission._
import com.xebialabs.xlrelease.stress.utils.JsUtils._
import spray.json._

case class Team(teamName: String,
                members: Seq[Member],
                permissions: Seq[Permission.Local] = Seq(Permission.ViewTemplate),
                systemTeam: Boolean = false,
                id: Team.ID = None)

object Team extends DefaultJsonProtocol {
  type ID = Option[String]

  implicit val teamShow: Show[Team] = {
    case Team(name, members, permissions, systemTeam, id) =>
      s"$name ${members.map(_.show).mkString("[", ", ", "]")} ${permissions.map((_: Permission).show).mkString("{", ", ", "}")} (system: ${systemTeam})"
  }

  implicit val teamWriter: RootJsonWriter[Team] = team => JsObject(
    "id" -> team.id.toJson,
    "teamName" -> team.teamName.toJson,
    "members" -> team.members.map(Member.memberWriter.write).toJson,
    "permissions" -> team.permissions.map(Permission.permissionWriter.write).toJson,
    "systemTeam" -> team.systemTeam.toJson
  )

  implicit val teamReader: RootJsonReader[Team] = json => {
    for {
      id <- getStringField("id")(json).map(_.value)
      name <- getStringField("teamName")(json).map(_.value)
      members <- (getField("members")(json) >>= getElements) map (_.map(_.convertTo[Member]))
      permissions <- (getField("permissions")(json) >>= getElements) map (_.map(_.convertTo[Permission.Local]))
      systemTeam <- getBooleanField("systemTeam")(json).map(_.value)
    } yield Team(name, members, permissions, systemTeam, Some(id))
  }.fold(x => throw x, x => x)

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

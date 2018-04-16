package com.xebialabs.xlrelease.stress.parsers.dataset

import com.xebialabs.xlrelease.stress.parsers.dataset.Member.UserMember
import spray.json._

sealed trait Member

object Member extends DefaultJsonProtocol {
  case class RoleMember(roleId: Role.ID) extends Member
  case class UserMember(userId: User.ID) extends Member

  implicit val memberWriter: RootJsonWriter[Member] = {
    case RoleMember(roleId) => JsObject(
      "name" -> roleId.toJson,
      "type" -> "ROLE".toJson
    )
    case UserMember(userId) => JsObject(
      "name" -> userId.toJson,
      "type" -> "PRINCIPAL".toJson
    )
  }

//  implicit val memberReader: RootJsonReader[Member] = {
//    case obj@JsObject(_) =>
//      obj.getFields("name", "type") match {
//        case Seq(JsString(name), JsString("ROLE")) => RoleMember(name)
//        case Seq(JsString(name), JsString("PRINCIPAL")) => UserMember(name)
//        case _ => deserializationError("Cannot extract fields from js object")
//      }
//    case _ => deserializationError("Cannot extract js object")
//  }

//  implicit val memberFormat: RootJsonFormat[Member] = rootJsonFormat(memberReader, memberWriter)
}

case class Team(id: Team.ID, teamName: String, members: Seq[Member],
                permissions: Seq[Permission with Permission.Local] = Seq(Permission.ViewTemplate), systemTeam: Boolean = false)

object Team extends DefaultJsonProtocol {
  type ID = Option[String]

  implicit val teamWriter: RootJsonWriter[Team] = team => JsObject(
    "id" -> team.id.toJson,
    "teamName" -> team.teamName.toJson,
    "members" -> team.members.map(Member.memberWriter.write).toJson,
    "permissions" -> team.permissions.map(Permission.permissionWriter.write).toJson,
    "systemTeam" -> team.systemTeam.toJson
  )


  def templateOwner(owner: User, templateId: Template.ID) = Team(Some("Applications/" + templateId + "/TeamWhateverA"), "Template Owner",
    Seq(UserMember(owner.username)), Permission.allLocalPermissions.toSeq, systemTeam = true)
  def releaseAdmin(owner: User, templateId: Template.ID) = Team(Some("Applications/" + templateId + "/TeamWhateverB"), "Release Admin",
    Seq(UserMember(owner.username)), Seq(Permission.ViewTemplate), systemTeam = true)

}

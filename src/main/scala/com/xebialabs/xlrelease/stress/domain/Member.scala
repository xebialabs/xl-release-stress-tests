package com.xebialabs.xlrelease.stress.domain

import cats._
import cats.implicits._

import com.xebialabs.xlrelease.stress.utils.JsUtils._

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

  implicit val memberReader: RootJsonReader[Member] = json => {
    for {
      name <- getStringField("name")(json).map(_.value)
      member <- getStringField("type")(json).map(_.value) >>= {
        case "PRINCIPAL" => UserMember(name).asRight
        case "ROLE" => RoleMember(name).asRight
        case other => wrongType(s"wrong team member type: $other", "PRINCIPAL | ROLE", JsString(other))
      }
    } yield member
  }.fold(x => throw x, identity)

  implicit val memberFormat: RootJsonFormat[Member] = rootJsonFormat(memberReader, memberWriter)
}

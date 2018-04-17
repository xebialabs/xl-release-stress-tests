package com.xebialabs.xlrelease.stress.parsers.dataset

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
}

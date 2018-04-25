package com.xebialabs.xlrelease.stress.dsl.libs.xlr

import cats._
import cats.implicits._
import akka.http.scaladsl.model.headers.{Cookie, `Set-Cookie`}
import cats.data.NonEmptyList
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrConfig, XlrServer}
import com.xebialabs.xlrelease.stress.domain.{HttpSession, Role, User}
import com.xebialabs.xlrelease.stress.dsl.DSL
import com.xebialabs.xlrelease.stress.utils.JsUtils
import freestyle.free._
import freestyle.free.implicits._
import spray.json._


class Users[F[_]](server: XlrServer, adminPassword: AdminPassword)(implicit protected val _api: DSL[F]) extends XlrLib[F] with DefaultJsonProtocol { self =>
  protected var _adminSession: Option[HttpSession] = None

  protected lazy val adminUser: User = User("admin", "", "", adminPassword.password)

  protected def adminLogin(): Program[HttpSession] =
    login(adminUser).map { session =>
      _adminSession = Some(session)
      session
    }

  def login(user: User): Program[HttpSession] =
    for {
      _ <- api.log.debug(s"login(${user.username})")
      resp <- lib.http.json.post.plain(server.root(_ / "login"),
        JsObject(
          "username" -> adminUser.username.toJson,
          "password" -> adminUser.password.toJson
        ))
      cookies <- getCookies(resp.headers[`Set-Cookie`])
      _ <- api.http.discard(resp)
      session = HttpSession(user, cookies.map(c => Cookie(c.cookie.name, c.cookie.value)))
    } yield session

  def admin(): Program[HttpSession] =
    _adminSession.fold(adminLogin())(_.pure[Program])

  def createUser(user: User): Program[User.ID] =
    admin() >>= { implicit session =>
      for {
        _ <- log.debug(s"createUser(${user.username})")
        resp <- lib.http.json.post(server.api(_ / "users" / user.username),
          JsObject(
            "fullName" -> user.fullName.toJson,
            "email" -> user.email.toJson,
            "loginAllowed" -> true.toJson,
            "password" -> user.password.toJson
          ))
        content <- api.http.parseJson(resp)
        userId <- lib.json.read(JsUtils.readUsername)(content)
      } yield userId
    }

  def createRole(role: Role): Program[Role.ID] =
    admin() >>= { implicit session =>
      for {
        _ <- log.debug(s"createRole(${role.roleName})")
        resp <- lib.http.json.post(server.api(_ / "roles" / role.roleName),
          JsObject(
            "name" -> role.roleName.toJson,
            "permissions" -> role.permissions.map(_.permission.toJson).toJson,
            "principals" -> role.principals.map(user => JsObject("username" -> user.username.toJson)).toJson
          ))
        _ <- api.http.discard(resp)
      } yield role.roleName
    }

  def deleteUser(userId: User.ID): Program[Unit] =
    admin() >>= { implicit session =>
      for {
        _ <- log.debug(s"deleteUser($userId)")
        resp <- api.http.delete(server.api(_ / "users" / userId))
        _ <- api.http.discard(resp)
      } yield ()
    }

  def deleteRole(roleId: Role.ID): Program[Unit] =
    admin() >>= { implicit session =>
      for {
        _ <- log.debug(s"deleteRole($roleId)")
        resp <- api.http.delete(server.api(_ / "roles" / roleId))
        _ <- api.http.discard(resp)
      } yield ()
  }

  protected def getCookies(headers: Seq[`Set-Cookie`]): Program[NonEmptyList[`Set-Cookie`]] =
    headers.toList.toNel.map(_.pure[Program]).getOrElse {
      api.error.error[NonEmptyList[`Set-Cookie`]](new RuntimeException("lib.xlr.users: No login cookies for you!"))
    }

}

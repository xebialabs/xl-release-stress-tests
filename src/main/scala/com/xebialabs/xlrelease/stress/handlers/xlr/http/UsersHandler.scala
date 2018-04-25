package com.xebialabs.xlrelease.stress.handlers.xlr.http

import cats._
import cats.implicits._
import akka.http.scaladsl.model.headers.{Cookie, `Set-Cookie`}
import cats.data.NonEmptyList
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.handlers.xlr.XlrRest
import com.xebialabs.xlrelease.stress.dsl.xlr
import com.xebialabs.xlrelease.stress.dsl.http.{Client, Http, HttpLib}
import com.xebialabs.xlrelease.stress.utils.JsUtils._
import freestyle.free._
import freestyle.free.implicits._
import spray.json._


class UsersHandler[F[_]]()
                        (implicit val
                         server: XlrServer,
                         adminPassword: AdminPassword,
                         client: Client[F],
                         http: Http[F]) extends XlrRest {

  val httpLib = new HttpLib[F]()

  type Target[A] = FreeS[F, A]

  implicit def usersHandler: xlr.Users.Handler[Target] = new xlr.Users.Handler[Target] with DefaultJsonProtocol {
    protected val adminUser = User("admin", "", "", adminPassword.password)

    protected var _adminSession: Option[HttpSession] = None

    def adminLogin(): Target[HttpSession] = login(adminUser).map { session =>
      _adminSession = Some(session)
      session
    }

    protected def login(user: User): Target[HttpSession] =
      for {
        _ <- http.log.debug(s"login(${user.username})")
        resp <- httpLib.client.postJSON0(root(_ / "login"),
          JsObject(
            "username" -> adminUser.username.toJson,
            "password" -> adminUser.password.toJson
          ))
        cookies <- getCookies(resp.headers[`Set-Cookie`])
        _ <- http.client.discard(resp)
        session = HttpSession(user, cookies.map(c => Cookie(c.cookie.name, c.cookie.value)))
      } yield session

    protected def admin(): Target[HttpSession] =
      _adminSession.fold(adminLogin())(_.pure[Target])

    protected def createUser(user: User): Target[User.ID] =
      admin() >>= { implicit session =>
        for {
          _ <- debug(s"createUser(${user.username})")
          resp <- httpLib.client.postJSON(api(_ / "users" / user.username),
            JsObject(
              "fullName" -> user.fullName.toJson,
              "email" -> user.email.toJson,
              "loginAllowed" -> true.toJson,
              "password" -> user.password.toJson
            ))
          content <- http.client.parseJson(resp)
          userId <- http.json.read(readUsername)(content)
        } yield userId
      }

    protected def createRole(role: Role): Target[Role.ID] =
      admin() >>= { implicit session =>
        for {
          _ <- debug(s"createRole(${role.roleName})")
          resp <- httpLib.client.postJSON(api(_ / "roles" / role.roleName),
            JsObject(
              "name" -> role.roleName.toJson,
              "permissions" -> role.permissions.map(_.permission.toJson).toJson,
              "principals" -> role.principals.map(user => JsObject("username" -> user.username.toJson)).toJson
            ))
          _ <- http.client.discard(resp)
        } yield role.roleName
      }

    protected def deleteUser(userId: User.ID): Target[Unit] =
      admin() >>= { implicit session =>
        for {
          _ <- debug(s"deleteUser($userId)")
          resp <- http.client.delete(api(_ / "users" / userId))
          _ <- http.client.discard(resp)
        } yield ()
      }

    protected def deleteRole(roleId: Role.ID): Target[Unit] =
      admin() >>= { implicit session =>
        for {
          _ <- debug(s"deleteRole($roleId)")
          resp <- http.client.delete(api(_ / "roles" / roleId))
          _ <- http.client.discard(resp)
        } yield ()
      }
  }


  private def getCookies(headers: Seq[`Set-Cookie`]): Target[NonEmptyList[`Set-Cookie`]] =
    headers.toList.toNel.map(_.pure[Target]).getOrElse {
      http.error.error[NonEmptyList[`Set-Cookie`]](new RuntimeException("No login cookies!"))
    }

  private def debug(msg: String)(implicit session: User.Session): Target[Unit] =
    http.log.debug(s"${session.user.username}: $msg")

}

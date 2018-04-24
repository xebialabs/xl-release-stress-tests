package com.xebialabs.xlrelease.stress.handlers.xlr.io

import akka.http.scaladsl.model.headers.{Cookie, `Set-Cookie`}
import akka.stream.Materializer
import cats.effect.IO
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.handlers.http.future.AkkaHttpClient
import com.xebialabs.xlrelease.stress.dsl.xlr.Users
import com.xebialabs.xlrelease.stress.handlers.xlr.XlrRest
import com.xebialabs.xlrelease.stress.utils.JsUtils._
import spray.json._

import scala.concurrent.ExecutionContext

class UsersHandler()
                  (implicit val
                   server: XlrServer,
                   adminPassword: AdminPassword,
                   client: AkkaHttpClient,
                   ec: ExecutionContext,
                   m: Materializer) extends XlrRest with DefaultJsonProtocol {

  implicit def usersHandler: Users.Handler[IO] = new Users.Handler[IO] {

    protected val adminUser = User("admin", "", "", adminPassword.password)

    protected var _adminSession: Option[HttpSession] = None

    def adminLogin(): IO[HttpSession] = login(adminUser).map { session =>
      _adminSession = Some(session)
      session
    }

    protected def admin(): IO[User.Session] =
      _adminSession.fold(adminLogin())(IO.pure)

    protected def login(user: User): IO[User.Session] =
      client.postJSON0(
        root(_ / "login"),
        JsObject(
          "username" -> user.username.toJson,
          "password" -> user.password.toJson
        )
      ).discard { resp =>
        val cookies = resp.headers[`Set-Cookie`].toList
        HttpSession(user, cookies.map(sc => Cookie(sc.cookie.name, sc.cookie.value)))
      }.io

    protected def createUser(user: User): IO[User.ID] =
      admin() >>= { implicit session =>
        client.postJSON(
          api(_ / "users" / user.username),
          JsObject(
            "fullName" -> user.fullname.toJson,
            "email" -> user.email.toJson,
            "loginAllowed" -> true.toJson,
            "password" -> user.password.toJson
          )
        ).asJson.io >>=
          readUsername
            .toIO(s"createUser(${user.show}): Cannot read username")
      }

    protected def createRole(role: Role): IO[Role.ID] =
      admin() >>= { implicit session =>
        client.postJSON(
          api(_ / "roles" / role.rolename),
          JsObject(
            "name" -> role.rolename.toJson,
            "permissions" -> role.permissions.map(_.permission.toJson).toJson,
            "principals" -> role.principals.map(user => JsObject("username" -> user.username.toJson)).toJson
          )).discard(_ => role.rolename).io
      }

    protected def deleteUser(userId: User.ID): IO[Unit] =
      admin() >>= { implicit session =>
        client.delete(api(_ / "users" / userId))
          .discardU.io
      }

    protected def deleteRole(roleId: Role.ID): IO[Unit] =
      admin() >>= { implicit session =>
        client.delete(api(_ / "roles" / roleId))
          .discardU.io
      }
  }

}

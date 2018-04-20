package com.xebialabs.xlrelease.stress.handlers.xlr.akkaClient

import cats.implicits._
import cats.effect.IO
import akka.http.scaladsl.model.headers.{Cookie, `Set-Cookie`}
import akka.stream.Materializer
import cats.Show
import com.xebialabs.xlrelease.stress.api.xlr.Users
import com.xebialabs.xlrelease.stress.domain.{HttpSession, Role, User}
import com.xebialabs.xlrelease.stress.utils.JsUtils._

import scala.concurrent.ExecutionContext

class UsersHandler(val admin: User)(implicit client: AkkaHttpXlrClient, ec: ExecutionContext, m: Materializer) { self =>

  implicit def usersHandler: Users.Handler[IO] = new Users.Handler[IO] {

    protected var _adminSession: Option[HttpSession] = None

    def adminLogin(): IO[HttpSession] = login(self.admin).map { session =>
      _adminSession = Some(session)
      session
    }

    protected def admin(): IO[User.Session] =
      _adminSession.fold(adminLogin())(IO.pure)

    protected def login(user: User): IO[User.Session] =
      client.login(user).discard { resp =>
        val cookies = resp.headers[`Set-Cookie`]
        HttpSession(user, cookies.map(sc => Cookie(sc.cookie.name, sc.cookie.value)))
      }.io

    protected def createUser(user: User): IO[User.ID] =
      admin() >>= { implicit session =>
        client.createUser(user)
          .asJson
          .io >>= readUsername
          .toIO(s"createUser(${user.show}): Cannot read username")
      }

    protected def createRole(role: Role): IO[Role.ID] =
      admin() >>= { implicit session =>
        client.createRole(role)
          .discard(_ => role.rolename)
          .io
      }

    protected def deleteUser(userId: User.ID): IO[Unit] =
      admin() >>= { implicit session =>
        client.deleteUser(userId)
          .discardU
          .io
      }

    protected def deleteRole(roleId: Role.ID): IO[Unit] =
      admin() >>= { implicit session =>
        client.deleteRole(roleId)
          .discardU
          .io
      }
  }

  implicit val showUser: Show[User] = {
    case User (username, fullname, email, _) =>
      val mail = if (email.isEmpty) "" else s" <$email>"
      val full = if (fullname.isEmpty) "" else s" ($fullname$mail)"
      s"$username$full"
  }
}

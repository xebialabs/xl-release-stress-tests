package com.xebialabs.xlrelease.stress.handlers.xlr.akkaClient

import cats.effect.IO
import akka.http.scaladsl.model.headers.{Cookie, `Set-Cookie`}
import akka.stream.Materializer
import com.xebialabs.xlrelease.stress.api.xlr.Users
import com.xebialabs.xlrelease.stress.domain.{HttpSession, Role, User}

import scala.concurrent.{ExecutionContext, Future}
import spray.json._

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

    protected def createUser(user: User): IO[User.ID] = {
      for {
        adminSession <- admin()
        userId <- client.createUser(user)(adminSession).asJson.io.flatMap {
          case JsObject(_) => IO.pure(user.username)
          case _ => IO.raiseError(new RuntimeException(s"Cannot create user ${user.username}"))
        }
      } yield userId
    }

    protected def createRole(role: Role): IO[Role.ID] = {
      for {
        adminSession <- admin()
        roleId <- client.createRole(role)(adminSession).discard(_ => role.rolename).io
      } yield roleId
    }

    protected def deleteUser(userId: User.ID): IO[Unit] = {
      for {
        adminSession <- admin()
        _ <- client.deleteUser(userId)(adminSession).io
      } yield ()
    }

    protected def deleteRole(roleId: Role.ID): IO[Unit] = {
      for {
        adminSession <- admin()
        _ <- client.deleteRole(roleId)(adminSession).io
      } yield ()
    }
  }

}

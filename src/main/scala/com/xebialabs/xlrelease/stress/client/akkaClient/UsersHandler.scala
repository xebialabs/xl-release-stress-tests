package com.xebialabs.xlrelease.stress.client.akkaClient

import akka.http.scaladsl.model.headers.{Cookie, `Set-Cookie`}
import akka.stream.Materializer
import com.xebialabs.xlrelease.stress.client.Users
import com.xebialabs.xlrelease.stress.parsers.dataset.{Role, User}

import scala.concurrent.{ExecutionContext, Future}
import spray.json._

class UsersHandler(val client: AkkaHttpXlrClient, val admin: User)(implicit ec: ExecutionContext, m: Materializer) { self =>

  implicit def usersHandler: Users.Handler[Future] = new Users.Handler[Future] {

    protected var _adminSession: Option[HttpSession] = None

    def adminLogin(): Future[HttpSession] = login(self.admin).map { session =>
      _adminSession = Some(session)
      session
    }

    protected def admin(): Future[User.Session] =
      _adminSession.fold(adminLogin())(Future.successful)

    protected def login(user: User): Future[User.Session] =
      client.login(user).discard { resp =>
        val cookies = resp.headers[`Set-Cookie`]
        HttpSession(user, cookies.map(sc => Cookie(sc.cookie.name, sc.cookie.value)))
      }

    protected def createUser(user: User): Future[User.ID] = {
      for {
        adminSession <- admin()
        userId <- client.createUser(user)(adminSession).asJson.collect {
          case JsObject(_) => Future.successful(user.username)
          case _ => Future.failed(new RuntimeException(s"Cannot create user ${user.username}"))
        }.flatten
      } yield userId
    }

    protected def createRole(role: Role): Future[Role.ID] = {
      for {
        adminSession <- admin()
        roleId <- client.createRole(role)(adminSession).discard(_ => role.rolename)
      } yield roleId
    }
  }

}

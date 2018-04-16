package com.xebialabs.xlrelease.stress.client.akkaClient

import com.xebialabs.xlrelease.stress.client.Users
import com.xebialabs.xlrelease.stress.parsers.dataset.{Role, User}

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.mutable.{HashMap => MutableHashMap}

class UsersHandler(val client: AkkaHttpXlrClient, val admin: User)(implicit ec: ExecutionContext) { self =>

  implicit def usersHandler: Users.Handler[Future] = new Users.Handler[Future] {
//    private val sessions: MutableHashMap[User.ID, User.Session] = MutableHashMap.empty

    protected var _adminSession: Option[HttpSession] = None

    def adminLogin(): Future[HttpSession] = client.login(self.admin).map { session =>
      _adminSession = Some(session)
      session
    }

    protected def admin(): Future[User.Session] =
      _adminSession.fold(adminLogin())(Future.successful)

    protected def login(user: User): Future[User.Session] =
      client.login(user)

    protected def createUser(user: User): Future[User.ID] = {
      for {
        adminSession <- admin()
        userId <- client.createUser(user)(adminSession)
      } yield userId
    }

    protected def createRole(role: Role): Future[Role.ID] = {
      for {
        adminSession <- admin()
        roleId <- client.createRole(role)(adminSession)
      } yield roleId
    }
  }

}

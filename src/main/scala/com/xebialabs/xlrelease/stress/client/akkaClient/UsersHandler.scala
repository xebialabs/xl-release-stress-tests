package com.xebialabs.xlrelease.stress.client.akkaClient

import com.xebialabs.xlrelease.stress.client.Users
import com.xebialabs.xlrelease.stress.parsers.dataset.User

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.mutable.{HashMap => MutableHashMap}

class UsersHandler(val client: AkkaHttpXlrClient, val admin: User)(implicit ec: ExecutionContext) { self =>

  implicit def usersHandler: Users.Handler[Future] = new Users.Handler[Future] {
    private val sessions: MutableHashMap[User.ID, HttpSession] = MutableHashMap.empty

    protected var _adminSession: Option[HttpSession] = None

    def adminLogin(): Future[HttpSession] = client.login(self.admin).map { session =>
      _adminSession = Some(session)
      session
    }

    protected def admin(): Future[HttpSession] =
      _adminSession.fold(adminLogin())(Future.successful)

    protected def createUser(user: User): Future[User.ID] = {
      println(s"CreateUserOp($user)")
      for {
        adminSession <- admin()
        userId <- client.createUser(user)(adminSession)
      } yield userId
    }

    protected def login(user: User): Future[HttpSession] = {
      println(s"LoginOp($user)")
      // not the actual session
      client.login(user).map { session =>
        sessions += user.username -> session
        session
      }
    }
  }

}

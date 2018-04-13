package com.xebialabs.xlrelease.stress.client.akkaClient

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import cats.~>
import com.xebialabs.xlrelease.stress.client.{Releases, Users}
import com.xebialabs.xlrelease.stress.parsers.dataset.User

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object XLRClientHandler {
  private val client = new AkkaHttpXlrClient(Uri("https://releaseit.xebialabs.com/xlr-master-integration"), BasicHttpCredentials("admin", "admin"))

  private var sessions: Map[User.ID, User] = Map.empty

  sessions = sessions + ("admin" -> User("admin", "", "", "admin"))


    implicit val usersClient: Users.Op ~> Future = new (Users.Op ~> Future) {
      def apply[A](fa: Users.Op[A]): Future[A] = fa match {
        case Users.CreateUserOp(user) =>
          Future.successful(user.username)

        case Users.LoginOp(user) =>
          // not the actual session
          sessions = sessions + (user.username -> user)
          Future.successful(user.username)
      }
    }

  implicit val releasesClient: Releases.Op ~> Future = new (Releases.Op ~> Future) {
    def apply[A](fa: Releases.Op[A]): Future[A] = fa match {
      case Releases.ImportTemplateOp(username, template) =>
        sessions.get(username) match {
          case None =>
            Future.failed(???)
          case Some(user) =>
            client.importTemplate(user, template).flatMap {
              case None =>
                Future.failed(???)
              case Some(id) =>
                Future.successful(id)
            }
        }

      case Releases.CreateReleaseOp(session, template) =>
        ???
    }
  }
}

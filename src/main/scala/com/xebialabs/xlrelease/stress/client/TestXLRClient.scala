package com.xebialabs.xlrelease.stress.client

import java.nio.file.Paths

import akka.http.scaladsl.model.Uri
import com.xebialabs.xlrelease.stress.client.akkaClient.{AkkaHttpXlrClient, ReleasesHandler, UsersHandler}
import com.xebialabs.xlrelease.stress.parsers.dataset._
import com.xebialabs.xlrelease.stress.parsers.dataset.Permission._
import freestyle.free._
import freestyle.free.implicits._
import cats.implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

@module trait XLRClient {
  val releases: Releases
  val users: Users
}

object TestXLRClient {

  object dsl extends DatasetDSL

  val client: AkkaHttpXlrClient = new AkkaHttpXlrClient(Uri("http://localhost:5516"))
  val usersInterpreter = new UsersHandler(client, User("admin", "", "", "admin"))
  val releaseInterpreter = new ReleasesHandler(client)

  // thanks NotSoSmartIdea
  implicit val usersHandler: Users.Handler[Future] = usersInterpreter.usersHandler
  implicit val releasesHandler: Releases.Handler[Future] = releaseInterpreter.releasesHandler

  val user1: User = dsl.user("user1", "user1")
  val role1: Role = dsl.role("role1",
    permissions = Set(CreateTemplate, CreateRelease, CreateTopLevelFolder),
    principals = Set(user1.username)
  )
  val template1: Template = dsl.template("test", Paths.get(this.getClass.getClassLoader.getResource("test-template.xlr").getPath))


  def scenario1[F[_]](implicit C: XLRClient[F]): FreeS[F, String] = {
    import C.{releases, users}

    for {
      session <- users.admin()
      _ <- users.createUser(user1)
      _ <- users.createRole(role1)
      templateId <- releases.importTemplate(session, user1, template1)
      userSession <- users.login(user1)
      releaseId <- releases.createRelease(userSession, templateId, CreateReleaseArgs("test", Map.empty, Map.empty))
    } yield releaseId
  }

  def main(args: Array[String]): Unit = {
    Await.result(
      scenario1[XLRClient.Op].interpret[Future].map { releaseId =>
        println("releaseId: " + releaseId)
      },
      100 seconds
    )
    Await.ready(client.shutdown(), 30 seconds)

  }


}

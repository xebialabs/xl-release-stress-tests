package com.xebialabs.xlrelease.stress.client

import java.nio.file.Paths

import akka.http.scaladsl.model.Uri
import com.xebialabs.xlrelease.stress.client.akkaClient.{AkkaHttpXlrClient, ReleasesHandler, UsersHandler}
import com.xebialabs.xlrelease.stress.parsers.dataset.{CreateReleaseArgs, Template, User}
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

object TestXLRClient extends App {

  val client: AkkaHttpXlrClient = new AkkaHttpXlrClient(Uri("http://localhost:5516"))
  val usersInterpreter = new UsersHandler(client, User("admin", "", "", "admin"))
  val releaseInterpreter = new ReleasesHandler(client)

  // thanks NotSoSmartIdea
  implicit val usersHandler: Users.Handler[Future] = usersInterpreter.usersHandler
  implicit val releasesHandler: Releases.Handler[Future] = releaseInterpreter.releasesHandler

  val user1 = User("user1", "", "", "user1")

  val template1 = Template("test", Paths.get(this.getClass.getClassLoader.getResource("test-template.xlr").getPath))


  def scenario1[F[_]](implicit C: XLRClient[F]): FreeS[F, String] = {
    import C.{releases, users}

    for {
      session <- users.admin()
      _ <- users.createUser(user1)
      templateId <- releases.importTemplate(session, template1)
      releaseId <- releases.createRelease(session, templateId, CreateReleaseArgs("test", Map.empty, Map.empty))
    } yield releaseId
  }

  Await.result(
    for {
      releaseId <- scenario1[XLRClient.Op].interpret[Future]
      _ <- client.shutdown()
    } yield {
      println("releaseId: " + releaseId)
    },
    300 seconds
  )
}
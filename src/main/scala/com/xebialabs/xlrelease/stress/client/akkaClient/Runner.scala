package com.xebialabs.xlrelease.stress.client.akkaClient

import cats.effect.IO
import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.logging._
import freestyle.free.loggingJVM.log4s.implicits._
import freestyle.free.nondeterminism._
import com.xebialabs.xlrelease.stress.client.{Program, Releases, Tasks, Users, XLRClient}
import com.xebialabs.xlrelease.stress.domain.User

import scala.concurrent.ExecutionContext.Implicits.global

object Runner {
  def runIO[A](program: Program[A])(implicit client: AkkaHttpXlrClient, API: XLRClient[XLRClient.Op]): IO[A] = {
    import client.materializer

    val usersInterpreter = new UsersHandler(User("admin", "", "", "admin"))
    val releaseInterpreter = new ReleasesHandler
    val tasksInterpreter = new TasksHandler

    implicit val usersHandler: Users.Handler[IO] = usersInterpreter.usersHandler
    implicit val releasesHandler: Releases.Handler[IO] = releaseInterpreter.releasesHandler
    implicit val tasksHandler: Tasks.Handler[IO] = tasksInterpreter.tasksHandler

    program.interpret[IO]
  }
}

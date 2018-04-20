package com.xebialabs.xlrelease.stress

import cats.effect.IO
import com.xebialabs.xlrelease.stress.api.{API, Program}
import com.xebialabs.xlrelease.stress.api.exec.Control
import com.xebialabs.xlrelease.stress.api.xlr.{Releases, Tasks, Users}
import com.xebialabs.xlrelease.stress.domain.User
import com.xebialabs.xlrelease.stress.handlers.exec.io.ExecHandler
import com.xebialabs.xlrelease.stress.handlers.xlr.akkaClient.{AkkaHttpXlrClient, ReleasesHandler, TasksHandler, UsersHandler}
import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.loggingJVM.log4s.implicits._

import scala.concurrent.ExecutionContext


object Runner {
  def runIO[A](program: Program[A])(implicit client: AkkaHttpXlrClient, API: API, ec: ExecutionContext): IO[A] = {
    import client.materializer

    val usersInterpreter = new UsersHandler(User("admin", "", "", "admin"))
    val releaseInterpreter = new ReleasesHandler
    val tasksInterpreter = new TasksHandler
    val engineInterpreter = new ExecHandler

    implicit val usersHandler: Users.Handler[IO] = usersInterpreter.usersHandler
    implicit val releasesHandler: Releases.Handler[IO] = releaseInterpreter.releasesHandler
    implicit val tasksHandler: Tasks.Handler[IO] = tasksInterpreter.tasksHandler
    implicit val engineHandler: Control.Handler[IO] = engineInterpreter.engineHandler

    program.interpret[IO]
  }
}

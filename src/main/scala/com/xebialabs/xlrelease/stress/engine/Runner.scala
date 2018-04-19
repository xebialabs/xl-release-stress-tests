package com.xebialabs.xlrelease.stress.engine

import cats.effect.IO
import com.xebialabs.xlrelease.stress.client.akkaClient.{AkkaHttpXlrClient, ReleasesHandler, TasksHandler, UsersHandler}
import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.logging._
import freestyle.free.loggingJVM.log4s.implicits._
import freestyle.free.nondeterminism._
import com.xebialabs.xlrelease.stress.client.{Releases, Tasks, Users}
import com.xebialabs.xlrelease.stress.domain.User
import com.xebialabs.xlrelease.stress.engine.ioEngine.EngineHandler

import scala.concurrent.ExecutionContext


object Runner {
  def runIO[A](program: HiProgram[A])(implicit client: AkkaHttpXlrClient, API: XLREngine[XLREngine.Op], ec: ExecutionContext): IO[A] = {
    import client.materializer

    val usersInterpreter = new UsersHandler(User("admin", "", "", "admin"))
    val releaseInterpreter = new ReleasesHandler
    val tasksInterpreter = new TasksHandler
    val engineInterpreter = new EngineHandler

    implicit val usersHandler: Users.Handler[IO] = usersInterpreter.usersHandler
    implicit val releasesHandler: Releases.Handler[IO] = releaseInterpreter.releasesHandler
    implicit val tasksHandler: Tasks.Handler[IO] = tasksInterpreter.tasksHandler
    implicit val engineHandler: Engine.Handler[IO] = engineInterpreter.engineHandler

    program.interpret[IO]
  }
}

package com.xebialabs.xlrelease.stress.handlers.io

import cats._
import cats.implicits._
import cats.effect.IO
import com.xebialabs.xlrelease.stress.api.exec.Control
import com.xebialabs.xlrelease.stress.api.xlr.{Releases, Tasks, Users}
import com.xebialabs.xlrelease.stress.api.{API, Program}
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.handlers.io.exec.ControlHandler
import com.xebialabs.xlrelease.stress.handlers.io.xlr.{ReleasesHandler, TasksHandler, UsersHandler}
import com.xebialabs.xlrelease.stress.http.AkkaHttpClient
import com.xebialabs.xlrelease.stress.scenarios.Scenario
import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.loggingJVM.log4s.implicits._

import scala.concurrent.ExecutionContext

trait Runner {
  def runIO[A](program: Program[A])
              (implicit
               server: XlrServer,
               admin: AdminPassword,
               client: AkkaHttpClient,
               ec: ExecutionContext): IO[A] = {
    import client.materializer

    val usersInterpreter = new UsersHandler
    val releaseInterpreter = new ReleasesHandler
    val tasksInterpreter = new TasksHandler
    val controlInterpreter = new ControlHandler

    implicit val usersHandler: Users.Handler[IO] = usersInterpreter.usersHandler
    implicit val releasesHandler: Releases.Handler[IO] = releaseInterpreter.releasesHandler
    implicit val tasksHandler: Tasks.Handler[IO] = tasksInterpreter.tasksHandler
    implicit val controlHandler: Control.Handler[IO] = controlInterpreter.controlHandler

    program.interpret[IO]
  }

  def runScenario(scenario: Scenario)
                 (implicit
                  server: XlrServer,
                  admin: AdminPassword,
                  client: AkkaHttpClient,
                  ec: ExecutionContext,
                  api: API): Unit = {

    val program = for {
      _ <- api.log.info(s"Running scenario: ${scenario.name}")
      _ <- scenario.program
      _ <- api.log.info(s"Scenario ${scenario.name} done")
    } yield ()

    (runIO(program) >> shutdown).unsafeRunSync()
  }

  def shutdown(implicit client: AkkaHttpClient, admin: AdminPassword): IO[Unit] = {
    for {
      _ <- IO(println("Shutting down akka-http client..."))
      _ <- IO.fromFuture(IO(client.shutdown()))
      _ <- IO(println("Shut down complete."))
    } yield ()
  }

}

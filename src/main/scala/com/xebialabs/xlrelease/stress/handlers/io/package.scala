package com.xebialabs.xlrelease.stress.handlers

import cats._
import cats.implicits._
import cats.effect.IO
import com.xebialabs.xlrelease.stress.dsl.{API, Program}
import com.xebialabs.xlrelease.stress.dsl.exec.Control
import com.xebialabs.xlrelease.stress.dsl.xlr.{Releases, Tasks, Users}
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.handlers.exec.ControlHandler
import com.xebialabs.xlrelease.stress.handlers.http.future.AkkaHttpClient
import com.xebialabs.xlrelease.stress.handlers.xlr.io.{ReleasesHandler, TasksHandler, UsersHandler}
import com.xebialabs.xlrelease.stress.scenarios.Scenario
import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.loggingJVM.log4s.implicits._

import scala.concurrent.ExecutionContext

package object io {
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

  def runScenario[A](scenario: Scenario[A])
                    (implicit
                     server: XlrServer,
                     admin: AdminPassword,
                     client: AkkaHttpClient,
                     ec: ExecutionContext,
                     api: API): Unit = {
    import scenario.showParams

    def info(msg: String): api.log.FS[Unit] = api.log.info(s"${scenario.name}: $msg")
    def warn(msg: String): api.log.FS[Unit] = api.log.warn(s"${scenario.name}: $msg")

    def error(err: => Throwable): Program[Unit] = for {
      _ <- api.log.error(s"${scenario.name}: Error during scenario execution: ${err.getMessage}")
      _ <- err.getCause.getStackTrace.map(t => warn(t.toString)).toList.sequence
    } yield ()

    val setup: Program[A] = for {
      _ <- info("setting up...")
      params <- scenario.setup
      _ <- info(s"setup complete: ${params.show}.")
    } yield params

    def program(params: A): Program[Unit] =
      for {
        _ <- info(s"Running scenario: ${scenario.name}")
        _ <- scenario.program(params)
        _ <- info(s"Scenario ${scenario.name} done")
      } yield ()

    def runProgram(params: A): IO[Unit] =
        runIO(program(params))
          .runCancelable {
            case Left(err) =>
              runIO(error(err))
            case Right(_) =>
              ().pure[IO]
          }.unsafeRunSync()

    val nop: Program[Unit] = ().pure[Program]

    def logClean(p: Program[Unit]): Program[Unit] =
      for {
        _ <- info(s"Cleaning up...")
        _ <- p
        _ <- info("cleanup complete.")
      } yield ()

    def runCleanup(params: A): IO[Unit] =
      runIO(logClean(scenario.cleanup(params)))

    val exec: IO[Unit] = for {
      params <- runIO(setup)
      _ <- runProgram(params)
      _ <- runCleanup(params)
    } yield ()

    exec.unsafeRunSync()
  }

  def shutdown(implicit client: AkkaHttpClient): IO[Unit] = {
    for {
      _ <- IO(println("Shutting down akka-http client..."))
      _ <- IO.fromFuture(IO(client.shutdown()))
      _ <- IO(println("Shut down complete."))
    } yield ()
  }
}

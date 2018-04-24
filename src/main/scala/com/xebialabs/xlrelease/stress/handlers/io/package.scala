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
  class RunnerContext()(implicit
                        val usersHandler: Users.Handler[IO],
                        val releasesHandler: Releases.Handler[IO],
                        val tasksHandler: Tasks.Handler[IO],
                        val ec: ExecutionContext)

  object RunnerContext {
    def controlHandler(implicit ctx: RunnerContext): Control.Handler[IO] = new ControlHandler().controlHandler
  }

  def runner(implicit
             server: XlrServer,
             admin: AdminPassword,
             client: AkkaHttpClient,
             ec: ExecutionContext): RunnerContext = {
    import client.materializer

    val usersInterpreter = new UsersHandler
    val releasesInterpreter = new ReleasesHandler
    val tasksInterpreter = new TasksHandler

    new RunnerContext()(
      usersInterpreter.usersHandler,
      releasesInterpreter.releasesHandler,
      tasksInterpreter.tasksHandler,
      ec)
  }

  def runIO[A](program: Program[A])
              (implicit ctx: RunnerContext): IO[A] = {
//    import client.materializer
    import ctx._
    implicit val ctrl: Control.Handler[IO] = RunnerContext.controlHandler

    program.interpret[IO]
  }

  def runScenario[A](scenario: Scenario[A])
                    (implicit ctx: RunnerContext, api: API): Unit = {
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
        _ <- info(s"Running scenario")
        r <- scenario.program(params)
        _ <- info(s"scenario program: $r")
        _ <- info(s"Scenario done")
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

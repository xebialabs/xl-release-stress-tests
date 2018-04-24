package com.xebialabs.xlrelease.stress.runners

import cats.effect.IO
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.dsl.exec.Control
import com.xebialabs.xlrelease.stress.dsl.xlr.{Releases, Tasks, Users}
import com.xebialabs.xlrelease.stress.dsl.{API, Program}
import com.xebialabs.xlrelease.stress.handlers.exec.io.ControlHandler
import com.xebialabs.xlrelease.stress.handlers.xlr.io.{ReleasesHandler, TasksHandler, UsersHandler}
import com.xebialabs.xlrelease.stress.scenarios.Scenario
import com.xebialabs.xlrelease.stress.utils.AkkaHttpClient
import freestyle.free._
import freestyle.free.implicits._

import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}

object io {
  class RunnerContext()(implicit
                        val usersHandler: Users.Handler[IO],
                        val releasesHandler: Releases.Handler[IO],
                        val tasksHandler: Tasks.Handler[IO],
                        val ec: ExecutionContext)

  object RunnerContext {
    def controlHandler(implicit ctx: RunnerContext): Control.Handler[IO] = ControlHandler.controlHandler
  }

  def runnerContext(implicit
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
    import ctx._
    import ControlHandler.controlHandler
    import freestyle.free.loggingJVM.log4s.implicits.taglessLoggingApplicative

    program.interpret[IO]
  }

  def runScenario[A](scenario: Scenario[A])
                    (implicit ctx: RunnerContext, api: API): Unit = {
    import scenario.showParams

    def info(msg: String): api.log.FS[Unit] = api.log.info(s"${scenario.name}: $msg")
    def warn(msg: String): api.log.FS[Unit] = api.log.warn(s"${scenario.name}: $msg")

    val setup: Program[A] = for {
      _ <- info("setting up...")
      params <- scenario.setup
      _ <- info(s"setup complete: ${params.show}.")
    } yield params

    def program(params: A): Program[Unit] =
      for {
        _ <- info(s"Running scenario")
        _ <- scenario.program(params)
        _ <- info(s"Scenario done")
      } yield ()

    def logClean(p: Program[Unit]): Program[Unit] =
      for {
        _ <- info(s"Cleaning up...")
        _ <- p
        _ <- info("cleanup complete.")
      } yield ()

    def runCleanup(params: A): IO[Unit] =
      runIO(logClean(scenario.cleanup(params)))

    val params: A = runIO(setup).unsafeRunSync()

    Try {
      runIO(program(params)).unsafeRunSync()
    }.recoverWith { case err =>
      println("!! Error while executing program : "+ err)
      Success(())
    }.map { _ =>
      runCleanup(params).unsafeRunSync()
    }
  }

  def shutdown(implicit client: AkkaHttpClient): IO[Unit] = {
    for {
      _ <- IO(println("Shutting down akka-http client..."))
      _ <- IO.fromFuture(IO(client.shutdown()))
      _ <- IO(println("Shut down complete."))
    } yield ()
  }
}

package com.xebialabs.xlrelease.stress.runners


import cats.effect.IO
import cats.implicits._
import cats.~>
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.dsl
import com.xebialabs.xlrelease.stress.handlers.io.{ControlHandler, HttpClientHandler}
import com.xebialabs.xlrelease.stress.scenarios.Scenario
import com.xebialabs.xlrelease.stress.utils.AkkaHttpClient
import freestyle.free._
import freestyle.free.implicits._

import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}

object io {
  class RunnerContext()(implicit
                        val httpClientHandler: dsl.http.Client.Handler[IO],
                        val controlHandler: dsl.Control.Handler[IO])

  def runnerContext(implicit
                    server: XlrServer,
                    admin: AdminPassword,
                    ec: ExecutionContext): RunnerContext = {
    val httpHandler = new HttpClientHandler()
    implicit val httpClientHandler: dsl.http.Client.Handler[IO] = httpHandler.clientHandler

    val controlHandler = new ControlHandler()
    implicit val handler: dsl.Control.Handler[IO] = controlHandler.controlHandler

    new RunnerContext()
  }

  def runIO[A](program: dsl.Program[A])
              (implicit ctx: RunnerContext): IO[A] = {
    import ctx._
    import freestyle.free.loggingJVM.log4s.implicits.taglessLoggingApplicative
    import freestyle.free.effects.error.implicits.freeStyleErrorMHandler

    program.interpret[IO]
  }

  def runScenario[A](scenario: Scenario[A])
                    (implicit ctx: RunnerContext, ec: ExecutionContext): Unit = {
    import scenario.showParams
    import scenario.api
    import scenario.Program

    def info(msg: String): Program[Unit] = api.log.info(s"${scenario.name}: $msg")
    def warn(msg: String): Program[Unit] = api.log.warn(s"${scenario.name}: $msg")

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
      err.printStackTrace()
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

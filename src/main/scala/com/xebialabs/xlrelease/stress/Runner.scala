package com.xebialabs.xlrelease.stress

import cats.effect.IO
import com.xebialabs.xlrelease.stress.api.{API, Program}
import com.xebialabs.xlrelease.stress.api.exec.Control
import com.xebialabs.xlrelease.stress.api.xlr.{Releases, Tasks, Users}
import com.xebialabs.xlrelease.stress.domain.User
import com.xebialabs.xlrelease.stress.handlers.exec.io.ControlHandler
import com.xebialabs.xlrelease.stress.handlers.xlr.akkaClient.{AkkaHttpXlrClient, ReleasesHandler, TasksHandler, UsersHandler}
import com.xebialabs.xlrelease.stress.scenarios.Scenario
import freestyle.free._
import freestyle.free.implicits._
import freestyle.free.loggingJVM.log4s.implicits._

import scala.concurrent.ExecutionContext

trait Runner {
  def runIO[A](program: Program[A])(implicit client: AkkaHttpXlrClient, ec: ExecutionContext): IO[A] = {
    import client.materializer

    val usersInterpreter = new UsersHandler(User("admin", "", "", "admin"))
    val releaseInterpreter = new ReleasesHandler
    val tasksInterpreter = new TasksHandler
    val controlInterpreter = new ControlHandler

    implicit val usersHandler: Users.Handler[IO] = usersInterpreter.usersHandler
    implicit val releasesHandler: Releases.Handler[IO] = releaseInterpreter.releasesHandler
    implicit val tasksHandler: Tasks.Handler[IO] = tasksInterpreter.tasksHandler
    implicit val controlHandler: Control.Handler[IO] = controlInterpreter.controlHandler

    program.interpret[IO]
  }
//
//  def run[A](program: Program[A])(implicit client: AkkaHttpXlrClient, ec: ExecutionContext): IO[Unit] = {
//    runIO(program)
//  }

  def runScenario(scenario: Scenario)(implicit client: AkkaHttpXlrClient, ec: ExecutionContext, api: API): Unit = {
    for {
      _ <- runIO {
        for {
          _ <- api.log.info(s"Running scenario: ${scenario.name}")
          _ <- scenario.program
          _ <- api.log.info(s"Scenario ${scenario.name} done")
        } yield ()
      }
      _ <- shutdown
    } yield ()
  }.unsafeRunSync()

  def shutdown(implicit client: AkkaHttpXlrClient): IO[Unit] = {
    for {
      _ <- IO(println("Shutting down akka-http client..."))
      _ <- IO.fromFuture(IO(client.shutdown()))
      _ <- IO(println("Shut down complete."))
    } yield ()
  }

}

object Runner {
  object instance extends Runner
}
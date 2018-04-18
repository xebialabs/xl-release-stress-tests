package com.xebialabs.xlrelease.stress.engine

import cats._
import cats.data._
import cats.free.{Free, FreeApplicative}
import cats.implicits._
import com.xebialabs.xlrelease.stress.client.{Releases, Tasks, Users, XLRClient}
import com.xebialabs.xlrelease.stress.client.akkaClient.{AkkaHttpXlrClient, ReleasesHandler, TasksHandler, UsersHandler}
import com.xebialabs.xlrelease.stress.domain.User
import freestyle.free._
// DO NOT REMOVE THIS IMPORT
import freestyle.free.implicits._
import freestyle.free.logging._
import freestyle.free.loggingJVM.log4s.implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

case class Scenario[A](name: String, program: Program[A])

object Scenario {
  def runFuture[A](scenario: Scenario[A])(implicit client: AkkaHttpXlrClient, API: XLRClient[XLRClient.Op]): Future[A] = {
    import client.materializer

    val usersInterpreter = new UsersHandler(client, User("admin", "", "", "admin"))
    val releaseInterpreter = new ReleasesHandler(client)
    val tasksInterpreter = new TasksHandler(client)

    // thanks NotSoSmartIdea
    implicit val usersHandler: Users.Handler[Future] = usersInterpreter.usersHandler
    implicit val releasesHandler: Releases.Handler[Future] = releaseInterpreter.releasesHandler
    implicit val tasksHandler: Tasks.Handler[Future] = tasksInterpreter.tasksHandler

    val programWithLogging: Program[A] = for {
      _ <- API.log.info(s"Starting scenario ${scenario.name}")
      a <- scenario.program
      _ <- API.log.info(s"Scenario ${scenario.name} completed")
    } yield a

    programWithLogging.interpret[Future]
  }

  implicit class ScenarioOps[A](val scenario: Scenario[A]) extends AnyVal {
    def run(implicit client: AkkaHttpXlrClient, API: XLRClient[XLRClient.Op]): Future[A] = runFuture(scenario)
  }


}

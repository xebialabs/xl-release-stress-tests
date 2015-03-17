package com.xebialabs.xlrelease

import com.xebialabs.xlrelease.client.XlrClient
import com.xebialabs.xlrelease.domain._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Main extends App {

  val client = new XlrClient("http://localhost:5516")

  println("Creating data in XLR repo...")

  private val users = Range(1, 201).map(n => "cdPerfUser%03d".format(n)).map(username => User(username, username))

  users.foreach {
    case user =>
      println(s"Creating $user")
      Await.result(printFuture(client.createUser(user)), 100 seconds)
  }

  printFuture(client.setRoles(Seq(Principal(Role(name = "PERFORMANCE"), users))))

  printFuture(client.setPermissions(Seq(Permission(Role(name = "PERFORMANCE"), Seq("admin")))))

  client.getPermissions("PERFORMANCE").onSuccess {
    case p: Permission =>
      val copy = p.copy(permissions = p.permissions :+ "admin")
      client.setPermissions(Seq(copy))
  }

  private def printFuture(f: Future[Any]): Future[Any] = {
    f.onComplete {
      case Success(t) => println(t)
      case Failure(e) => e.printStackTrace()
    }

    f
  }


}

package com.xebialabs.xlrelease.stress

import java.util.concurrent._

import cats._
import cats.implicits._
import akka.http.scaladsl.model.Uri
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrConfig, XlrServer}
import com.xebialabs.xlrelease.stress.domain.Template

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

object Main {
  val usage: String =
    """
      |    sbt "run xlReleaseUrl adminPassword numUsers"
      |
      |example:
      |    sbt "run http://xl-release.xebialabs.com:5516 admin 10"
    """.stripMargin


  def main(args: Array[String]): Unit = {
    if (args.length < 5) {
      println(usage)
      System.exit(-1)
    }

    val hostname = Uri(args(0))
    val adminPassword = args(1)
//    val numUsers = args(2).toInt
//    val threads = 2 * Math.max(1, numUsers)

    val parallelism: Int = args(2).toInt
    val parGroups: Int = args(3).toInt
    val parTasks: Int = args(4).toInt
    val threads = 20

    implicit val config: XlrConfig = XlrConfig(
      server = XlrServer(hostname),
      adminPassword = AdminPassword(adminPassword)
    )

    val pool: ExecutorService = Executors.newFixedThreadPool(threads)
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(pool)

    scenarios.CommentsScenario(parallelism, parGroups, parTasks).run

    sys.exit(0)
  }


}


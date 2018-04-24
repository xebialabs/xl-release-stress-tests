package com.xebialabs.xlrelease.stress

import java.util.concurrent._

import akka.http.scaladsl.model.Uri
import com.xebialabs.xlrelease.stress.config.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.handlers.http.future.AkkaHttpClient

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

object Main {
  val usage: String =
    """
      |    sbt "run xlReleaseUrl adminPassword numUsers"
      |
      |example:
      |    sbt "run http://xl-release.xebialabs.com:5516 admin 100"
    """.stripMargin


  def main(args: Array[String]): Unit = {
    if (args.length < 3) {
      println(usage)
      System.exit(-1)
    }

    val hostname = Uri(args(0))
    val adminPassword = args(1)
    val numUsers = args(2).toInt
    val threads = 2 * Math.max(1, numUsers)

    implicit val server: XlrServer = XlrServer(hostname)
    implicit val client: AkkaHttpClient = new AkkaHttpClient()
    implicit val admin: AdminPassword = AdminPassword(adminPassword)

    val pool: ExecutorService = Executors.newFixedThreadPool(threads)
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(pool)

    scenarios
      .CreateReleases(admin).run
//      .CompleteReleases(1).run

    pool.shutdown()

    sys.exit(0)
  }


}


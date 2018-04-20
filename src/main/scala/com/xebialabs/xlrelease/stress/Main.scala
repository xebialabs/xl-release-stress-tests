package com.xebialabs.xlrelease.stress

import java.util.concurrent._

import akka.http.scaladsl.model.Uri
import com.xebialabs.xlrelease.stress.domain.{AdminPassword, XlrServer}
import com.xebialabs.xlrelease.stress.handlers.akkaClient.AkkaHttpXlrClient

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

object Main extends Runner {
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

    implicit val server: XlrServer = XlrServer(hostname)
    implicit val client: AkkaHttpXlrClient = new AkkaHttpXlrClient()
    implicit val admin: AdminPassword = AdminPassword(adminPassword)

    val pool: ExecutorService = Executors.newFixedThreadPool(2 * numUsers)
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(pool)

    scenarios
      .CompleteReleases(numUsers)
//      .CreateReleases
      .run

    pool.shutdown()

    sys.exit(0)
  }


}


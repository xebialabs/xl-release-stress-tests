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
      |    sbt "run http://xl-release.xebialabs.com:5516 admin howMany templateId"
    """.stripMargin


  def main(args: Array[String]): Unit = {
    if (args.length < 4) {
      println(usage)
      System.exit(-1)
    }

    val hostname = Uri(args(0))
    val adminPassword = args(1)
//    val numUsers = args(2).toInt
//    val threads = 2 * Math.max(1, numUsers)

    val howMany: Int = args(2).toInt
    val templateId: Template.ID = Template.ID(args(3))
    val threads = 20

    implicit val config: XlrConfig = XlrConfig(
      server = XlrServer(hostname),
      adminPassword = AdminPassword(adminPassword)
    )

    val pool: ExecutorService = Executors.newFixedThreadPool(threads)
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(pool)

    scenarios.TestSomething(templateId, howMany).run

//    scenarios.TestSomething().run
//    scenarios.CompleteReleases(numUsers).run
//    val programs = List(
//      scenarios.Generate(numUsers, ReleaseStatus.Planned),
//      scenarios.Generate(numUsers, ReleaseStatus.Failed),
//      scenarios.Generate(numUsers, ReleaseStatus.InProgress),
//      scenarios.Generate(numUsers, ReleaseStatus.Completed),
//      scenarios.Generate(numUsers, ReleaseStatus.Aborted)
//    )
//
//    val p = programs.head
//
//    val full = for {
//      params <- p.setup
//      _ <- programs.map(_.program(params)).sequence
//      _ <- p.cleanup(params)
//    } yield ()
//    io.runIO(full)(io.runnerContext(config.server, config.adminPassword, ec)).unsafeRunSync()

//    pool.shutdown()

    sys.exit(0)
  }


}


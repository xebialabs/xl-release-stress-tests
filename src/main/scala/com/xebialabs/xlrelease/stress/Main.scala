package com.xebialabs.xlrelease.stress

import java.io.{File, FileOutputStream}
import java.util.concurrent._

import akka.http.scaladsl.model.Uri
import com.xebialabs.xlrelease.stress.domain.Template
import com.xebialabs.xlrelease.stress.handlers.xlr.akkaClient.AkkaHttpXlrClient
import com.xebialabs.xlrelease.stress.utils.ResourceManagement.using
import org.apache.commons.io.IOUtils

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps

object Main {
  val usage: String =
    """
      |    sbt run xlReleaseUrl numUsers
      |
      |example:
      |    sbt run http://xl-release.xebialabs.com:5516 100
    """.stripMargin


  def shutdown(client: AkkaHttpXlrClient): Unit = {
    println("shutting down")
    Await.ready(client.shutdown(), 20 seconds)
    println("shutting down complete")
    System.exit(0)
  }


  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      println(usage)
      System.exit(-1)
    }

    val hostname = Uri(args(0))
    val numUsers = args(1).toInt

    implicit val client: AkkaHttpXlrClient = new AkkaHttpXlrClient(hostname)

    val templateFile: File = {
      val in = this.getClass.getClassLoader.getResourceAsStream("DSL_1mb.xlr")
      val tmpFile = File.createTempFile("stress-test", ".xlr")
      tmpFile.deleteOnExit()
      using(new FileOutputStream(tmpFile)) { out =>
        IOUtils.copy(in, out)
      }
      tmpFile
    }

    val template: Template = Template("test", templateFile)

    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(
      Executors.newFixedThreadPool(2 * numUsers)
    )

    Runner.runIO {
      TestScenarios.fullScenario(template, numUsers)
    }.unsafeRunSync()

    shutdown(client)

  }


}


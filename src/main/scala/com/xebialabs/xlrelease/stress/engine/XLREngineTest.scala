package com.xebialabs.xlrelease.stress.engine

import java.nio.file.Paths

import akka.http.scaladsl.model.Uri
import com.xebialabs.xlrelease.stress.client.TestScenarios
import com.xebialabs.xlrelease.stress.client.TestScenarios._
import com.xebialabs.xlrelease.stress.client.akkaClient.AkkaHttpXlrClient
import com.xebialabs.xlrelease.stress.domain.Template

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps

object XLREngineTest {
  val xlrServer = Uri("http://leningrad.xebialabs.com:5516")

  def main(args: Array[String]): Unit = {
    implicit val client: AkkaHttpXlrClient = new AkkaHttpXlrClient(xlrServer)

    val template: Template = Template("test", Paths.get(this.getClass.getClassLoader.getResource("DSL2.xlr").getPath))

    //    Await.result(
    //      seqScenario(template, 100).run,
    //      (20 * 100) seconds
    //    )

    def shutdown(): Unit = {
      println("shutting down")
      Await.ready(client.shutdown(), 5 seconds)
      println("shutting down complete")
      System.exit(0)
    }

    Runner.runIO {
      TestScenarios.fullScenario(template, numUsers = 100)
    }.unsafeRunSync()

    shutdown()

  }


}


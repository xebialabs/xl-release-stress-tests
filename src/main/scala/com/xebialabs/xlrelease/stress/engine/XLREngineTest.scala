package com.xebialabs.xlrelease.stress.engine

import java.nio.file.Paths
import java.util.concurrent._

import akka.http.scaladsl.model.Uri
import com.xebialabs.xlrelease.stress.client.TestScenarios
import com.xebialabs.xlrelease.stress.client.akkaClient.AkkaHttpXlrClient
import com.xebialabs.xlrelease.stress.domain.Template

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps

object XLREngineTest {
  val xlrServer = Uri("http://aiki.xebialabs.com:5516")

  def main(args: Array[String]): Unit = {
    implicit val client: AkkaHttpXlrClient = new AkkaHttpXlrClient(xlrServer)

    val template: Template = Template("test", Paths.get(this.getClass.getClassLoader.getResource("DSL2.xlr").getPath))

    def shutdown(): Unit = {
      println("shutting down")
      Await.ready(client.shutdown(), 5 seconds)
      println("shutting down complete")
      System.exit(0)
    }

    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(
      Executors.newFixedThreadPool(200)
    )

    Runner.runIO {
      TestScenarios.fullScenario(template, numUsers = 20)
    }.unsafeRunSync()

    shutdown()

  }


}


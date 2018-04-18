package com.xebialabs.xlrelease.stress.client

import java.nio.file.Paths

import akka.http.scaladsl.model.Uri
import com.xebialabs.xlrelease.stress.client.TestScenarios.parScenario
import com.xebialabs.xlrelease.stress.client.akkaClient.AkkaHttpXlrClient
import com.xebialabs.xlrelease.stress.domain.{Role, Template, User}
import com.xebialabs.xlrelease.stress.domain.Permission._
import com.xebialabs.xlrelease.stress.engine.{Program, Scenario}

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps

object XLRClientTest {
  val xlrServer = Uri("http://localhost:5516")

  def main(args: Array[String]): Unit = {
    implicit val client: AkkaHttpXlrClient = new AkkaHttpXlrClient(xlrServer)

    val template: Template = Template("test", Paths.get(this.getClass.getClassLoader.getResource("DSL2.xlr").getPath))

    Await.result(
      parScenario(template, 100).run,
      (20 * 100) seconds
    )

    println("shutting down")
    Await.ready(client.shutdown(), 5 seconds)
    println("shutting down complete")

    System.exit(0)
  }


}


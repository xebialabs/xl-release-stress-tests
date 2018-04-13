package com.xebialabs.xlrelease.stress.client

import java.nio.file.Paths

import com.xebialabs.xlrelease.stress.parsers.dataset.Template
import freestyle.free._

import scala.concurrent.ExecutionContext.Implicits.global
import cats.instances.future._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

//@module trait XLRClient {
//  val releases: Releases
//  val users: Users
//}


object TestXLRClient extends App {


  def scenario1[F[_]](implicit C: Releases[F]): FreeS[F, String] = {
    import C._

    for {
      templateId <- importTemplate("admin", Template("test", Paths.get("/home/icassina/dev/xl/xlr-stress-tests/src/main/resources/test-template.json").toFile))
    } yield templateId
  }

  println("running program")
  import com.xebialabs.xlrelease.stress.client.akkaClient.XLRClientHandler._
  Await.result(
    scenario1[Releases.Op].interpret[Future].map { templateId =>
      println("templateId: " + templateId)
    },
    300 seconds
  )



}
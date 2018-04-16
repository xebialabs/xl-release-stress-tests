package com.xebialabs.xlrelease.stress.client

import java.nio.file.Paths

import com.xebialabs.xlrelease.stress.parsers.dataset.{CreateReleaseArgs, Template}
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

    val template1 = Template("test", Paths.get(this.getClass.getClassLoader.getResource("test-template.xlr").getPath))
    for {
      templateId <- importTemplate("admin", template1)
      releaseId <- createRelease("admin", templateId, CreateReleaseArgs("test", Map.empty, Map.empty))
    } yield releaseId
  }

  println("running program")
  import com.xebialabs.xlrelease.stress.client.akkaClient.XLRClientHandler._
  Await.result(
    scenario1[Releases.Op].interpret[Future].map { releaseId =>
      println("releaseId: " + releaseId)
    },
    300 seconds
  )


}

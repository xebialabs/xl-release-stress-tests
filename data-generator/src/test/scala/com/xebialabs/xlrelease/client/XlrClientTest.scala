package com.xebialabs.xlrelease.client

import com.xebialabs.xlrelease.domain._
import com.xebialabs.xlrelease.support.UnitTestSugar
import org.scalatest.FunSuite
import spray.http.{HttpResponse, StatusCodes, StatusCode}

import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

class XlrClientTest extends UnitTestSugar {

  val client = new XlrClient("http://localhost:5516")

  describe("XLR client") {
    it("should create a release") {
      val release = Release.build("Release004")

      val createResponse = client.createRelease(release).futureValue
      createResponse.status shouldBe StatusCodes.OK

      val removeResponse = client.removeCi(release.id).futureValue
      removeResponse.status shouldBe StatusCodes.NoContent
    }

    it("should create a phase within release") {
      val release = Release.build("Release005")

      client.createRelease(release).onComplete {
        case Success(r) =>
          val phase = Phase.build("Phase001", release.id)
          val createPhaseResponse = client.createPhase(phase).futureValue
          createPhaseResponse.status shouldBe StatusCodes.OK
          val removeResponse = client.removeCi(phase.id).futureValue
          removeResponse.status shouldBe StatusCodes.NoContent
        case Failure(ex) =>
          throw ex
      }
    }

    it("should create tasks") {
      val release = Release.build("Release002")
      val phase = Phase.build("Phase002", release.id)

      val taskResponse = for (
        releaseResponse <- client.createRelease(release);
        phaseResponse <- client.createPhase(phase);
        taskResponse <- client.createTask(Task.build("Task002", phase.id))
      ) yield taskResponse

      taskResponse.futureValue.status shouldBe StatusCodes.OK

      client.removeCi(release.id)
    }

    it("should create many releases in batches") {
      val range = 0 until 20
      val releases = range.map(id =>
        Release.build(s"Release$id")
      )
      val groups = releases.grouped(100).toSeq

      val releaseResponsesFutures = groups.map(client.createReleases)
      expectSuccessfulResponses(releaseResponsesFutures)

      val releaseRemovalFutures = releases.map(release => {
        client.removeCi(release.id)
      })
      expectSuccessfulResponses(releaseRemovalFutures)
    }

    it("should create many releases") {
      val range = 0 until 20
      val releases = range.map(id =>
        Release.build(s"Release$id")
      )
      val releaseResponsesFutures = releases.map(client.createRelease)
      expectSuccessfulResponses(releaseResponsesFutures)

      val releaseRemovalFutures = releases.map(release => {
        client.removeCi(release.id)
      })
      expectSuccessfulResponses(releaseRemovalFutures)
    }
  }

  def expectSuccessfulResponses(responsesFutures: Seq[Future[HttpResponse]]): Unit = {
    val releaseResponses = Future.sequence(responsesFutures).futureValue
    releaseResponses.foreach(releaseResponse =>
      releaseResponse.status.intValue should (be >= 200 and be < 300)
    )
  }
}

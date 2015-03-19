package com.xebialabs.xlrelease.client

import com.xebialabs.xlrelease.XlrJsonProtocol
import com.xebialabs.xlrelease.domain._
import com.xebialabs.xlrelease.support.UnitTestSugar
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import spray.http.{HttpResponse, StatusCodes}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class XlrClientTest extends UnitTestSugar with XlrJsonProtocol {

  val client = new XlrClient("http://localhost:5516")

  describe("XLR client") {
    it("should create a release") {
      val release = Release.build("ReleaseTest004")

      val createResponse = client.createRelease(release).futureValue
      createResponse.status shouldBe StatusCodes.OK

      val removeResponse = client.removeCi(release.id).futureValue
      removeResponse.status shouldBe StatusCodes.NoContent
    }

    it("should create a phase within release") {
      val release = Release.build("ReleaseTest005")

      val phase = Phase.build("Phase002", release.id)

      val phaseResponse = for (
        releaseResponse <- client.createRelease(release);
        phaseResponse <- client.createPhase(phase)
      ) yield phaseResponse

      phaseResponse.futureValue.status shouldBe StatusCodes.OK

      client.removeCi(release.id)
    }

    it("should create tasks") {
      val release = Release.build("ReleaseTest102")
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
        Release.build(s"ReleaseTest$id")
      )
      val groups = releases.grouped(100).toSeq

      val releaseResponsesFutures = groups.map {
        case group: Seq[Release] => client.createCis(group.toSeq)
      }
      expectSuccessfulResponses(releaseResponsesFutures)

      val releaseRemovalFutures = releases.map(release => {
        client.removeCi(release.id)
      })
      expectSuccessfulResponses(releaseRemovalFutures)
    }

    it("should create many releases") {
      val range = 0 until 20
      val releases = range.map(id =>
        Release.build(s"ReleaseTest$id")
      )
      val releaseResponsesFutures = releases.map(client.createRelease)
      expectSuccessfulResponses(releaseResponsesFutures)

      val releaseRemovalFutures = releases.map(release => {
        client.removeCi(release.id)
      })
      expectSuccessfulResponses(releaseRemovalFutures)
    }

    it("should import template from a file") {
      val createResponseFuture = client.importTemplate("/20-automated-tasks.xlr")
      expectSuccessfulResponse(createResponseFuture)
    }
  }

  def expectSuccessfulResponses(responsesFutures: Seq[Future[HttpResponse]]): Unit = {
    val releaseResponses = Future.sequence(responsesFutures).futureValue
    releaseResponses.foreach(releaseResponse =>
      releaseResponse.status.intValue should (be >= 200 and be < 300)
    )
  }

  def expectSuccessfulResponse(responseFuture: Future[HttpResponse]): Unit = {
    val response = responseFuture.futureValue
    response.status.intValue should (be >= 200 and be < 300)
  }
}

package com.xebialabs.xlrelease.client

import com.xebialabs.xlrelease.client.XlrClient._
import com.xebialabs.xlrelease.domain._
import com.xebialabs.xlrelease.generator.SpecialDayGenerator
import com.xebialabs.xlrelease.json.XlrJsonProtocol
import com.xebialabs.xlrelease.support.UnitTestSugar
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import spray.http.{HttpEntity, HttpResponse, StatusCodes}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class XlrClientTest extends UnitTestSugar with XlrJsonProtocol {

  val client = new XlrClient("http://localhost:5516")

  describe("XLR client") {
    it("should create a release") {
      val release = Release.build("ReleaseTest004")

      client.createRelease(release).futureValue.status shouldBe StatusCodes.NoContent

      client.removeCi(release.id).futureValue.status shouldBe StatusCodes.NoContent
    }

    it("should create a phase within release") {
      val release = Release.build("ReleaseTest005")
      release.phases = Seq(Phase.build("Phase002", release.id))

      client.createRelease(release).futureValue.status shouldBe StatusCodes.NoContent

      client.removeCi(release.id)
    }

    it("should create tasks") {
      val release = Release.build("ReleaseTest103")
      val phase = Phase.build("Phase002", release.id)
      val task = Task.build("Task002", phase.id)
      release.phases = Seq(phase)
      phase.tasks = Seq(task)

      client.createRelease(release).futureValue.status shouldBe StatusCodes.NoContent

      client.removeCi(release.id)
    }

    it("should create releases with activity logs") {
      val release = Release.build("ReleaseTest103")
      val logDirectory = ActivityLogDirectory.build(release.id)
      val logEntry = ActivityLogEntry.build(logDirectory.id, message = "Hello!")
      val releaseAndRelatedCis = ReleaseAndRelatedCis(release, Seq(logDirectory, logEntry))

      val createReleaseAndLogs = client.createReleaseAndRelatedCis(releaseAndRelatedCis)
      createReleaseAndLogs.futureValue.status shouldBe StatusCodes.NoContent

      client.removeCi(release.id)
      client.removeCi(logDirectory.id)
    }

    it("should create special days") {
      val days = SpecialDayGenerator.generateSpecialDays()

      expectSuccessfulResponse(client.createCis(days))

      val removalsFuture = days.map(_.id).map(client.removeCi)
      expectSuccessfulResponses(removalsFuture)
    }

    it("should create many CIs in batches") {
      val range = 0 until 20
      val cis = range.map(id =>
        ActivityLogDirectory.build(s"Applications/ReleaseTest$id")
      )
      val groups = cis.grouped(10).toSeq

      val responsesFutures = groups.map {
        group: Seq[Ci] => client.createCis(group)
      }
      expectSuccessfulResponses(responsesFutures)

      val removalFutures = cis.map(ci => {
        client.removeCi(ci.id)
      })
      expectSuccessfulResponses(removalFutures)
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

    it("should not fail if a release already exists") {
      val release = Release.build("ReleaseTest004")
      client.createRelease(release).futureValue

      // Check future is not failed on second create request
      client.createRelease(release).futureValue.status shouldBe StatusCodes.BadRequest

      client.removeCi(release.id).futureValue.status shouldBe StatusCodes.NoContent
    }

    it("should import template from a file") {
      val createResponseFuture = client.importTemplate("/many-automated-tasks.xlr")
      expectSuccessfulResponse(createResponseFuture)
    }

    it("should fail future of non-successful responses") {

      val response = HttpResponse(StatusCodes.BadRequest, HttpEntity("Some bad thing has happened."))

      val changedFuture = failNonSuccessfulResponses(Future.successful(response))

      whenReady(changedFuture.failed)(ex => ex.getMessage shouldBe "Some bad thing has happened.")
    }

    it("should not modify the future with successful response") {
      val response = HttpResponse(StatusCodes.OK, HttpEntity("Great success."))
      val changedFuture = failNonSuccessfulResponses(Future.successful(response))
      changedFuture.futureValue shouldBe response
    }

    it("should create server configurations") {
      val server = HttpConnection(s"Configuration/Custom/ConfigurationJenkins", "Jenkins", "jenkins.Server")
      val cis = Seq(server)

      expectSuccessfulResponse(client.createCis(cis))

      val removalsFuture = cis.map(_.id).map(client.removeCi)
      expectSuccessfulResponses(removalsFuture)
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

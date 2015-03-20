package com.xebialabs.xlrelease.client

import java.io.InputStream

import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import com.xebialabs.xlrelease.domain._
import com.xebialabs.xlrelease.json.XlrJsonProtocol
import spray.client.pipelining._
import spray.http.{BasicHttpCredentials, _}
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import XlrClient._

object XlrClient {

  /**
   * A wrapper for error messages extracted from non-successful responses.
   */
  class XlrClientException(m: String) extends RuntimeException(m)

  /**
   * Returns a failed [[Future]] for all the non-successful responses.
   */
  private [client] def failNonSuccessfulResponses(responseFuture: Future[HttpResponse]) = responseFuture.flatMap {
    case response if response.status.isFailure =>
      Future.failed(new XlrClientException(response.entity.data.asString))
    case _ =>
      responseFuture
  }
}

class XlrClient(apiUrl: String, username: String = "admin", password: String = "admin") extends XlrJsonProtocol with AdditionalFormats with LazyLogging {

  implicit val system: ActorSystem = ActorSystem()
  implicit val timeout: Timeout = Timeout(60.minutes)

  private val strictPipeline = (req: HttpRequest) => {
    val loggingResp = (i: HttpResponse) => logger.debug(i.toString)
    val loggingReq = (i: HttpRequest) => logger.debug(i.toString)

    val pipeline = logRequest(loggingReq) ~>
      addCredentials(BasicHttpCredentials(username, password)) ~>
      sendReceive ~>
      logResponse(loggingResp)

    failNonSuccessfulResponses(pipeline(req))
  }

  def createUser(u: User): Future[HttpResponse] = strictPipeline(Post(s"$apiUrl/users", u))

  def setRoles(roles: Seq[Principal]): Future[HttpResponse] = strictPipeline(Put(s"$apiUrl/roles/principals", roles))

  def getPermissions(roleName: String): Future[Permission] = strictPipeline(Get(s"$apiUrl/roles/permissions/global"))
    .map(obj => {
    obj.entity.as[JsObject] match {
      case Right(r) =>
        val roles = r.fields("rolePermissions").asInstanceOf[JsArray]
        roles.elements.find(r => r.asJsObject.fields("role").asJsObject.fields("name").asInstanceOf[JsString].value == roleName).get.convertTo[Permission]
      case Left(v) => null
    }
  })

  def setPermissions(permissions: Seq[Permission]): Future[HttpResponse] = strictPipeline(Put(s"$apiUrl/roles/permissions/global", permissions))

  def createRelease(release: Release): Future[HttpResponse] =
    strictPipeline(Post(s"$apiUrl/repository/ci/${release.id}", release))

  def createCis(cis: Seq[Ci]): Future[HttpResponse] = {

    val jsValues = cis.map {
      case ci: Release => ci.toJson
      case ci: Phase => ci.toJson
      case ci: Task => ci.toJson
      case ci: Dependency => ci.toJson
      case ci => throw new IllegalArgumentException(s"Undefined CI type ${ci.getClass}")
    }

    val data = JsArray(jsValues.toVector)

    strictPipeline(Post(s"$apiUrl/fixtures/", data))
  }

  def removeCi(id: String): Future[HttpResponse] =
    strictPipeline(Delete(s"$apiUrl/repository/ci/$id"))


  def createPhase(phase: Phase): Future[HttpResponse] =
    strictPipeline(Post(s"$apiUrl/repository/ci/${phase.id}", phase))

  def createTask(task: Task): Future[HttpResponse] =
    strictPipeline(Post(s"$apiUrl/repository/ci/${task.id}", task))
  
  def createDependency(dependency: Dependency): Future[HttpResponse] =
    strictPipeline(Post(s"$apiUrl/repository/ci/${dependency.id}", dependency))

  def importTemplate(file: String): Future[HttpResponse] = {
    val is: InputStream = getClass.getResourceAsStream(file)
    val bytes = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray
    val formFile = FormFile(file, HttpEntity(HttpData(bytes)).asInstanceOf[HttpEntity.NonEmpty])
    val mfd = MultipartFormData(Seq(BodyPart(formFile, "file")))

    strictPipeline(Post(s"$apiUrl/api/v1/templates/import", mfd))
  }

}

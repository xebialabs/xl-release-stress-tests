package com.xebialabs.xlrelease.client

import java.io.{InputStream, File}

import akka.actor.ActorSystem
import akka.util.{ByteString, Timeout}
import com.typesafe.scalalogging.LazyLogging
import com.xebialabs.xlrelease._
import com.xebialabs.xlrelease.domain._
import spray.client.pipelining._
import spray.http.HttpEntity.NonEmpty
import spray.http.{BasicHttpCredentials, _}
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.{BufferedSource, Source}
import scala.language.postfixOps

class XlrClient(apiUrl: String) extends XlrJsonProtocol with AdditionalFormats with LazyLogging {


  implicit val system: ActorSystem = ActorSystem()
  implicit val timeout: Timeout = Timeout(60.minutes)

  private val loggingReq = (i: HttpRequest) => logger.debug(i.toString)
  private val loggingResp = (i: HttpResponse) => logger.debug(i.toString)

  private val credentials = BasicHttpCredentials("admin", "admin")
  private val pipeline = logRequest(loggingReq) ~> addCredentials(credentials) ~> sendReceive ~> logResponse(loggingResp)

  def createUser(u: User): Future[HttpResponse] = pipeline(Post(s"$apiUrl/users", u))

  def setRoles(roles: Seq[Principal]): Future[HttpResponse] = pipeline(Put(s"$apiUrl/roles/principals", roles))

  def getPermissions(roleName: String): Future[Permission] = pipeline(Get(s"$apiUrl/roles/permissions/global"))
    .map(obj => {
    obj.entity.as[JsObject] match {
      case Right(r) =>
        val roles = r.fields("rolePermissions").asInstanceOf[JsArray]
        roles.elements.find(r => r.asJsObject.fields("role").asJsObject.fields("name").asInstanceOf[JsString].value == roleName).get.convertTo[Permission]
      case Left(v) => null
    }
  })

  def setPermissions(permissions: Seq[Permission]): Future[HttpResponse] = pipeline(Put(s"$apiUrl/roles/permissions/global", permissions))

  def createRelease(release: Release): Future[HttpResponse] =
    pipeline(Post(s"$apiUrl/repository/ci/${release.id}", release))



  def createCis(cis: Seq[Any]): Future[HttpResponse] = {

    val jsValues = cis.map {
      case ci: Release => ci.toJson
      case ci: Phase => ci.toJson
      case ci: Task => ci.toJson
      case ci => throw new IllegalArgumentException(s"Undefined CI type ${ci.getClass}")
    }

    val data = JsArray(jsValues.toVector)

    pipeline(Post(s"$apiUrl/fixtures/", data))
  }

  def removeCi(id: String): Future[HttpResponse] =
    pipeline(Delete(s"$apiUrl/repository/ci/$id"))


  def createPhase(phase: Phase): Future[HttpResponse] =
    pipeline(Post(s"$apiUrl/repository/ci/${phase.id}", phase))

  def createTask(task: Task): Future[HttpResponse] =
    pipeline(Post(s"$apiUrl/repository/ci/${task.id}", task))


  def importTemplate(file: String): Future[HttpResponse] = {
    val is: InputStream = getClass.getResourceAsStream(file)
    val bytes = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray
    val formFile = FormFile(file, HttpEntity(HttpData(bytes)).asInstanceOf[HttpEntity.NonEmpty])
    val mfd = MultipartFormData(Seq(BodyPart(formFile, "file")))

    pipeline(Post(s"$apiUrl/api/v1/templates/import", mfd))
  }


  //  release with dependency
  //  phases
  //  tasks
  //  gates
  //  special days

}

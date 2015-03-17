package com.xebialabs.xlrelease.client

import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import com.xebialabs.xlrelease._
import com.xebialabs.xlrelease.domain._
import spray.client.pipelining._
import spray.http.{BasicHttpCredentials, _}
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

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

  def createReleases(releases: Seq[Release]): Future[HttpResponse] =
    pipeline(Post(s"$apiUrl/fixtures/", releases))

  def removeCi(id: String): Future[HttpResponse] =
    pipeline(Delete(s"$apiUrl/repository/ci/$id"))


  def createPhase(phase: Phase): Future[HttpResponse] =
    pipeline(Post(s"$apiUrl/repository/ci/${phase.id}", phase))

  def createTask(task: Task): Future[HttpResponse] =
    pipeline(Post(s"$apiUrl/repository/ci/${task.id}", task))

//  release with dependency
//  phases
//  tasks
//  gates
//  special days

}

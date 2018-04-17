package com.xebialabs.xlrelease.stress.client.akkaClient

import java.nio.file.Path

import com.github.nscala_time.time.Imports._
import cats.implicits._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.MediaTypes.{`application/json`, `application/zip`}
import akka.http.scaladsl.model.{DateTime => _, _}
import akka.http.scaladsl.model.headers.Accept
import akka.stream.ActorMaterializer
import com.xebialabs.xlrelease.stress.client.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.client.utils.DateFormat
import com.xebialabs.xlrelease.stress.parsers.dataset._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class AkkaHttpXlrClient(val serverUri: Uri) extends SprayJsonSupport with DefaultJsonProtocol with DateFormat {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  protected val xlrApiPath: Uri.Path = serverUri.path / "api" / "v1"

  def shutdown(): Future[Unit] = Http().shutdownAllConnectionPools()

  def createUser(user: User)(implicit session: HttpSession): Future[HttpResponse] = {
    postJSON(serverUri.withPath(xlrApiPath / "users" / user.username), JsObject(
      "fullName" -> user.fullname.toJson,
      "email" -> user.email.toJson,
      "loginAllowed" -> true.toJson,
      "password" -> user.password.toJson
    ))
  }

  def login(user: User): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(POST, serverUri.withPath(serverUri.path / "login"),
      entity = HttpEntity(`application/json`, JsObject(
        "username" -> user.username.toJson,
        "password" -> user.password.toJson).compactPrint),
      headers = List(Accept(`application/json`))
    ))
  }

  def createRole(role: Role)(implicit session: HttpSession): Future[HttpResponse] = {
    postJSON(serverUri.withPath(xlrApiPath / "roles" / role.rolename), JsObject(
      "name" -> role.rolename.toJson,
      "permissions" -> role.permissions.map(_.permission.toJson).toJson,
      "principals" -> role.principals.map(username => JsObject("username" -> username.toJson)).toJson
    ))
  }

  def importTemplate(template: Template)(implicit session: HttpSession): Future[HttpResponse] = {
    postZip(serverUri.withPath(xlrApiPath / "templates" / "import"),
      template.xlrTemplate
    )
  }

  def setTemplateTeams(templateId: Template.ID, teams: List[Team])(implicit session: HttpSession): Future[HttpResponse] = {
    postJSON(serverUri.withPath(xlrApiPath / "templates" / "Applications" / templateId / "teams"),
      teams.map(_.toJson).toJson
    )
  }

  def setTemplateScriptUser(templateId: Template.ID, scriptUser: User)(implicit session: HttpSession): Future[HttpResponse] = {
    putJSON(serverUri.withPath(xlrApiPath / "templates" / "Applications" / templateId), JsObject(
      "id" -> JsNull,
      "scheduledStartDate" -> DateTime.now().toJson,
      "type" -> "xlrelease.Release".toJson,
      "scriptUsername" -> scriptUser.username.toJson,
      "scriptUserPassword" -> scriptUser.password.toJson
    ))
  }

  def createRelease(templateId: Template.ID, release: CreateReleaseArgs)(implicit session: HttpSession): Future[HttpResponse] = {
    postJSON(serverUri.withPath(xlrApiPath / "templates" / "Applications" / templateId / "create"),
      release.toJson
    )
  }

  def startRelease(releaseId: Release.ID)(implicit session: HttpSession): Future[HttpResponse] = {
    postJSON(serverUri.withPath(xlrApiPath / "releases" / "Applications" / releaseId / "start"), JsNull)
  }

  def getTaskByTitle(releaseId: Release.ID, taskTitle: String, phaseTitle: Option[String] = None)(implicit session: HttpSession): Future[JsValue] = {
    val baseQuery = Uri.Query(
      "releaseId" -> s"Applications/$releaseId",
      "taskTitle" -> taskTitle
    )
    getJSON(serverUri.withPath(xlrApiPath / "tasks" / "byTitle").withQuery(
      phaseTitle.fold(baseQuery)(pt => ("phaseTitle" -> pt) +: baseQuery)
    ))
  }

  def getJSON(uri: Uri)(implicit session: HttpSession): Future[JsValue] = {
    Http().singleRequest(HttpRequest(GET, uri, headers = Accept(`application/json`) :: session.cookies.toList))
      .flatMap(_.entity.asJson[JsValue])
  }

  def postJSON(uri: Uri, entity: JsValue)(implicit session: HttpSession): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(POST, uri,
      entity = HttpEntity(`application/json`, entity.compactPrint),
      headers = Accept(`application/json`) :: session.cookies.toList
    ))
  }

  def putJSON(uri: Uri, entity: JsValue)(implicit session: HttpSession): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(PUT, uri,
      entity = HttpEntity(`application/json`, entity.compactPrint),
      headers = Accept(`application/json`) :: session.cookies.toList
    ))
  }

  def postZip(uri: Uri, path: Path)(implicit session: HttpSession): Future[HttpResponse] = {
    val payload = Multipart.FormData(
      Multipart.FormData.BodyPart.fromPath("file", `application/zip`, path)
    )
    Http().singleRequest(HttpRequest(POST, uri,
      entity = payload.toEntity(),
      headers = Accept(`application/json`) :: session.cookies.toList
    ))
  }

}

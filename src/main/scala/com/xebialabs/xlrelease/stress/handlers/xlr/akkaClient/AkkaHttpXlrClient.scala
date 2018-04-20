package com.xebialabs.xlrelease.stress.handlers.xlr.akkaClient

import java.io.File
import java.net.URI
import java.nio.file.Path

import com.github.nscala_time.time.Imports._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.MediaTypes.{`application/json`, `application/zip`}
import akka.http.scaladsl.model.{DateTime => _, _}
import akka.http.scaladsl.model.headers.Accept
import akka.stream.ActorMaterializer
import com.xebialabs.xlrelease.stress.api.xlr.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.utils.DateFormat
import com.xebialabs.xlrelease.stress.domain._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class AkkaHttpXlrClient(val serverUri: Uri) extends SprayJsonSupport with DefaultJsonProtocol with DateFormat {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  protected val xlrApiPath: Uri.Path = serverUri.path / "api" / "v1"

  def shutdown(): Future[Unit] = Http().shutdownAllConnectionPools()

  def createUser(user: User)(implicit session: HttpSession): Future[HttpResponse] =
    postJSON(serverUri.withPath(xlrApiPath / "users" / user.username), JsObject(
      "fullName" -> user.fullname.toJson,
      "email" -> user.email.toJson,
      "loginAllowed" -> true.toJson,
      "password" -> user.password.toJson
    ))

  def deleteUser(userId: User.ID)(implicit session: HttpSession): Future[HttpResponse] =
    delete(serverUri.withPath(xlrApiPath / "users" / userId))

  def login(user: User): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(POST, serverUri.withPath(serverUri.path / "login"),
      entity = HttpEntity(`application/json`, JsObject(
        "username" -> user.username.toJson,
        "password" -> user.password.toJson).compactPrint),
      headers = List(Accept(`application/json`))
    ))

  def createRole(role: Role)(implicit session: HttpSession): Future[HttpResponse] =
    postJSON(serverUri.withPath(xlrApiPath / "roles" / role.rolename), JsObject(
      "name" -> role.rolename.toJson,
      "permissions" -> role.permissions.map(_.permission.toJson).toJson,
      "principals" -> role.principals.map(user => JsObject("username" -> user.username.toJson)).toJson
    ))

  def deleteRole(roleId: Role.ID)(implicit session: HttpSession): Future[HttpResponse] =
    delete(serverUri.withPath(xlrApiPath / "roles" / roleId))

  def importTemplate(template: Template)(implicit session: HttpSession): Future[HttpResponse] =
    postZip(serverUri.withPath(xlrApiPath / "templates" / "import"),
      template.xlrTemplate
    )

  def setTemplateTeams(templateId: Template.ID, teams: List[Team])(implicit session: HttpSession): Future[HttpResponse] =
    postJSON(serverUri.withPath(xlrApiPath / "templates" / "Applications" / templateId / "teams"),
      teams.map(_.toJson).toJson
    )

  def setTemplateScriptUser(templateId: Template.ID, scriptUser: User)(implicit session: HttpSession): Future[HttpResponse] =
    putJSON(serverUri.withPath(xlrApiPath / "templates" / "Applications" / templateId), JsObject(
      "id" -> JsNull,
      "scheduledStartDate" -> DateTime.now().toJson,
      "type" -> "xlrelease.Release".toJson,
      "scriptUsername" -> scriptUser.username.toJson,
      "scriptUserPassword" -> scriptUser.password.toJson
    ))

  def createReleaseFromTemplate(templateId: Template.ID, release: CreateReleaseArgs)(implicit session: HttpSession): Future[HttpResponse] =
    postJSON(serverUri.withPath(xlrApiPath / "templates" / "Applications" / templateId / "create"),
      release.toJson
    )

  def createRelease(title: String, scriptUser: User)(implicit session: HttpSession): Future[HttpResponse] =
    postJSON(serverUri.withPath(serverUri.path/ "releases"), JsObject(
      "templateId" -> JsNull,
      "title" -> title.toJson,
      "scheduledStartDate" -> DateTime.now().toJson,
      "dueDate" -> DateTime.now().plus(5.hours.toDuration).toJson,
      "type" -> "xlrelease.Release".toJson,
      "owner" -> JsObject(
        "username" -> session.user.username.toJson
      ),
      "scriptUsername" -> JsObject(
        "username" -> scriptUser.username.toJson
      ),
      "scriptUserPassword" -> scriptUser.password.toJson
    ))

  def getRelease(releaseId: Release.ID)(implicit session: HttpSession): Future[JsValue] =
    getJSON(serverUri.withPath(xlrApiPath / "releases" / "Applications" / releaseId))

  def startRelease(releaseId: Release.ID)(implicit session: HttpSession): Future[HttpResponse] =
    postJSON(serverUri.withPath(xlrApiPath / "releases" / "Applications" / releaseId / "start"), JsNull)

  def getTaskByTitle(releaseId: Release.ID, taskTitle: String, phaseTitle: Option[String] = None)
                    (implicit session: HttpSession): Future[JsValue] =
    Uri.Query(
      "releaseId" -> s"Applications/$releaseId",
      "taskTitle" -> taskTitle
    ) match { case baseQuery =>
      getJSON(serverUri.withPath(xlrApiPath / "tasks" / "byTitle").withQuery(
        phaseTitle.fold(baseQuery)(pt => ("phaseTitle" -> pt) +: baseQuery)
      ))
    }

  def pollTask(taskId: String)(implicit session: HttpSession): Future[HttpResponse] =
    postJSON(serverUri.withPath(serverUri.path / "tasks" / "poll"), JsObject(
      "ids" -> Seq(taskId).toJson
    ))

  def appendScriptTask(phaseId: Phase.ID, title: String, taskType: String, script: String)
                      (implicit session: HttpSession): Future[HttpResponse] =
    postJSON(
      serverUri.withPath(xlrApiPath / "tasks" / "Applications" / phaseId.releaseId / phaseId.phaseId / "tasks"),
      JsObject(
        "id" -> JsNull,
        "title" -> title.toJson,
        "type" -> taskType.toJson,
        "script" -> script.toJson
      )
    )

  def assignTaskTo(taskId: Task.ID, assignee: User.ID)(implicit session: HttpSession): Future[HttpResponse] =
    postJSON(
      serverUri.withPath(xlrApiPath / "tasks" / "Applications" / taskId.releaseId / taskId.phaseId.phaseId / taskId.taskId / "assign" / assignee),
      JsNull
    )

  def completeTask(taskId: Task.ID, comment: Option[String])(implicit session: HttpSession): Future[HttpResponse] =
    postJSON(
      serverUri.withPath(xlrApiPath / "tasks" / "Applications" / taskId.releaseId / taskId.phaseId.phaseId / taskId.taskId / "complete"),
      comment.map(content => JsObject("comment" -> content.toJson)).getOrElse(JsObject.empty)
    )

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

  def postZip(uri: Uri, content: File)(implicit session: HttpSession): Future[HttpResponse] = {
    val payload = Multipart.FormData(
      Multipart.FormData.BodyPart.fromFile(name = "file", `application/zip`, content)
    )
    Http().singleRequest(HttpRequest(POST, uri,
      entity = payload.toEntity(),
      headers = Accept(`application/json`) :: session.cookies.toList
    ))
  }

  def delete(uri: Uri)(implicit session: HttpSession): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(DELETE, uri,
      headers = Accept(`application/json`) :: session.cookies.toList
    ))
  }

}

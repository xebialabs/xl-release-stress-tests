package com.xebialabs.xlrelease.stress.client.akkaClient

import java.nio.file.Path

import cats.implicits._
import cats.instances.option._
import cats.instances.future._
import cats.syntax.traverse._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.MediaTypes.{`application/json`, `application/zip`}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.ActorMaterializer
import com.xebialabs.xlrelease.stress.parsers.dataset._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class AkkaHttpXlrClient(val serverUri: Uri) extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  protected val xlrApiPath: Uri.Path = serverUri.path / "api" / "v1"

  def shutdown(): Future[Unit] = Http().shutdownAllConnectionPools()

  def createUser(user: User)(implicit session: HttpSession): Future[User.ID] = {
    postJSONasJson(serverUri.withPath(xlrApiPath / "users" / user.username), JsObject(
      "fullName" -> user.fullname.toJson,
      "email" -> user.email.toJson,
      "loginAllowed" -> true.toJson,
      "password" -> user.password.toJson
    )).collect {
        case JsObject(_) => Future.successful(user.username)
        case _ => Future.failed(new RuntimeException(s"Cannot create user ${user.username}"))
      }.flatten
  }

  def login(user: User): Future[HttpSession] = {
    Http().singleRequest(HttpRequest(POST, serverUri.withPath(serverUri.path / "login"),
      entity = HttpEntity(`application/json`, JsObject(
        "username" -> user.username.toJson,
        "password" -> user.password.toJson).compactPrint),
      headers = List(Accept(`application/json`))
    )).map { resp =>
      val cookies = resp.headers[`Set-Cookie`]
      resp.discardEntityBytes()
      HttpSession(user, cookies.map(sc => Cookie(sc.cookie.name, sc.cookie.value)))
    }
  }

  def createRole(role: Role)(implicit session: HttpSession): Future[Role.ID] = {
    postJSON(serverUri.withPath(xlrApiPath / "roles" / role.rolename), JsObject(
      "name" -> role.rolename.toJson,
      "permissions" -> role.permissions.map(_.permission.toJson).toJson,
      "principals" -> role.principals.map(username => JsObject("username" -> username.toJson)).toJson
    )).map { resp =>
      resp.discardEntityBytes()
      role.rolename
    }
  }

  def importTemplate(template: Template)(implicit session: HttpSession): Future[Template.ID] = {
    postZip(serverUri.withPath(xlrApiPath / "templates" / "import"),
      template.xlrTemplate
    ).map[Option[String]] {
      case JsArray(ids) =>
        ids.headOption.flatMap(_.asJsObject.getFields("id").headOption).flatMap {
          case JsString(id) => Some(id)
          case _ => Option.empty[Template.ID]
        }
      case _ => Option.empty[Template.ID]
    }.flatMap(_.fold(Future.failed[Template.ID](new RuntimeException("Cannot extract Template ID")))(Future.successful))
  }

  def createRelease(templateId: Template.ID, release: CreateReleaseArgs)(implicit session: HttpSession): Future[Release.ID] = {
    postJSONasJson(serverUri.withPath(serverUri.path / "api" / "v1" / "templates" / "Applications" / templateId / "create"),
      release.toJson
    ).flatMap {
      case JsObject(r) => r.get("id") match {
        case Some(JsString(id)) => Future.successful(id)
        case _ => Future.failed(new RuntimeException("Cannot extract Release ID"))
      }
      case _ => Future.failed(new RuntimeException("not a Js object"))
    }
  }

  def postJSON(uri: Uri, entity: JsValue)(implicit session: HttpSession): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(POST, uri,
      entity = HttpEntity(`application/json`, entity.compactPrint),
      headers = Accept(`application/json`) :: session.cookies.toList
    ))
  }

  def postJSONasJson(uri: Uri, entity: JsValue)(implicit session: HttpSession): Future[JsValue] =
    postJSON(uri, entity).flatMap(_.entity.asJson[JsValue])

  def postZip(uri: Uri, path: Path)(implicit session: HttpSession): Future[JsValue] = {
    val payload = Multipart.FormData(
      Multipart.FormData.BodyPart.fromPath("file", `application/zip`, path)
    )
    for {
      resp <- Http().singleRequest(HttpRequest(POST, uri,
        entity = payload.toEntity(),
        headers = Accept(`application/json`) :: session.cookies.toList
      ))
      json <- resp
        .onSuccess(_.asJson[JsValue])
        .onFailure((status, msg) => new RuntimeException(s"Failed with status $status: $msg"))
    } yield json
  }

}

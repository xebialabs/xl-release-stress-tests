package com.xebialabs.xlrelease.stress.client.akkaClient

import java.nio.file.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.MediaTypes.{`application/json`, `application/zip`}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.ActorMaterializer
import com.xebialabs.xlrelease.stress.parsers.dataset.{Template, User}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class AkkaHttpXlrClient(val serverUri: Uri) extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  protected val xlrApiPath: Uri.Path = serverUri.path / "api" / "v1"

  def shutdown(): Future[Unit] = Http().shutdownAllConnectionPools()

  def importTemplate(template: Template)(implicit session: HttpSession): Future[Template.ID] = {
    postZip(serverUri.withPath(xlrApiPath / "templates" / "import"),
      template.xlrTemplate
    ).collect {
      case JsArray(ids) =>
        ids.headOption
          .flatMap(_.asJsObject.getFields("id").headOption)
          .map(_.toString) match {
          case None => Future.failed(new RuntimeException("Cannot read template id"))
          case Some(id) => Future.successful(id)
        }
      case _ => Future.failed(new RuntimeException("Not a json array"))
    }.flatten
  }

  def createUser(user: User)(implicit session: HttpSession): Future[User.ID] = {
    postJSON(serverUri.withPath(xlrApiPath / "users" / user.username), JsObject(
      "fullName" -> user.fullname.toJson,
      "email" -> user.email.toJson,
      "loginAllowed" -> true.toJson,
      "password" -> user.password.toJson
    ))
      .collect {
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

  def postJSON(uri: Uri, entity: JsValue)(implicit session: HttpSession): Future[JsValue] = {
    Http().singleRequest(HttpRequest(POST, uri,
      entity = HttpEntity(`application/json`, entity.compactPrint),
      headers = Accept(`application/json`) :: session.cookies.toList
    )).flatMap(_.entity.asJson[JsValue])
  }

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

package com.xebialabs.xlrelease.stress.client.akkaClient

import java.nio.file.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.MediaTypes.{`application/json`, `application/zip`}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Accept, Authorization, BasicHttpCredentials}
import akka.stream.ActorMaterializer
import com.xebialabs.xlrelease.stress.parsers.dataset.{Template, User}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class AkkaHttpXlrClient(serverUri: Uri, adminCredentials: BasicHttpCredentials) extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  def postJSON(uri: Uri, entity: JsValue)(implicit auth: Authorization): Future[JsValue] = {
    Http().singleRequest(HttpRequest(POST, uri,
      entity = HttpEntity(`application/json`, entity.compactPrint),
      headers = List(auth, Accept(`application/json`))
    )).flatMap(_.entity.asJson[JsValue])
  }

  def postZip(uri: Uri, path: Path)(implicit auth: Authorization): Future[JsValue] = {
    val payload = Multipart.FormData(
      Multipart.FormData.BodyPart.fromPath("file", `application/zip`, path)
    )
    for {
      resp <- Http().singleRequest(HttpRequest(POST, uri,
        entity = payload.toEntity(),
        headers = List(auth, Accept(`application/json`))
      ))
      json <- resp
        .onSuccess(_.asJson[JsValue])
        .onFailure((status, msg) => new RuntimeException(s"Failed with status $status: $msg"))
    } yield json

  }

  def importTemplate(user: User, template: Template): Future[Option[Template.ID]] = {
    postZip(serverUri.withPath(serverUri.path / "api" / "v1" / "templates" / "import"),
      template.xlrTemplate
    )(Authorization(adminCredentials))
      .collect {
        case JsArray(ids) => ids.headOption.flatMap(_.asJsObject.getFields("id").headOption.map(_.toString))
        case _ => None
      }
  }

}

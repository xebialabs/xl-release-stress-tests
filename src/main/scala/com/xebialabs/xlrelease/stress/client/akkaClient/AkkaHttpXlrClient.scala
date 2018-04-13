package com.xebialabs.xlrelease.stress.client.akkaClient

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Accept, Authorization, BasicHttpCredentials}
import akka.stream.ActorMaterializer
import com.xebialabs.xlrelease.stress.parsers.dataset.{Template, User}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

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

  def importTemplate(user: User, template: Template): Future[Option[Template.ID]] = {
    val jsonTemplate = Source.fromFile(template.jsonTemplate).mkString
      postJSON(
        serverUri.withPath(serverUri.path / "api" / "v1" / "templates"),
        jsonTemplate.parseJson
      )(Authorization(adminCredentials))
      .map(_.asJsObject.getFields("id").headOption.map(_.toString()))
  }

}

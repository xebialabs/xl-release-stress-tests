package com.xebialabs.xlrelease.json
import com.xebialabs.xlrelease.domain._
import spray.json._

trait XlrJsonProtocol extends DefaultJsonProtocol with AdditionalFormats with ZonedDateTimeProtocol {
  this: ProductFormatsInstances =>

  implicit val releaseFormat = jsonFormat8(Release.apply)
  implicit val phaseFormat = jsonFormat5(Phase.apply)
  implicit val taskFormat = jsonFormat4(Task.apply)
  implicit val dependencyFormat = jsonFormat3(Dependency.apply)
  implicit val userFormat = jsonFormat5(User)
  implicit val roleFormat = jsonFormat2(Role)
  implicit val puserFormat = jsonFormat2(PUser)
  implicit val principalFormat = jsonFormat2(Principal)
  implicit val permissionFormat = jsonFormat2(Permission)

  implicit object CiProtocol extends RootJsonFormat[Ci] {
    def read(json: JsValue): Ci = {
      deserializationError("Read is not implemented")
    }

    def write(obj: Ci): JsValue = {
      obj match {
        case ci: Release => ci.toJson
        case ci: Phase => ci.toJson
        case ci: Task => ci.toJson
        case ci: Dependency => ci.toJson
        case _ => serializationError(s"Undefined CI type ${obj.getClass}")
      }
    }
  }
}

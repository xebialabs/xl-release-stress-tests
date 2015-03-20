package com.xebialabs.xlrelease.json
import com.xebialabs.xlrelease.domain._
import spray.json.{AdditionalFormats, DefaultJsonProtocol, ProductFormatsInstances}

trait XlrJsonProtocol extends DefaultJsonProtocol with AdditionalFormats with DateTimeProtocol {
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
}

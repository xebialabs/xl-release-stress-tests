package com.xebialabs.xlrelease.json
import com.xebialabs.xlrelease.domain._
import spray.json._

trait XlrJsonProtocol extends DefaultJsonProtocol with AdditionalFormats with ZonedDateTimeProtocol {
  this: ProductFormatsInstances =>

  implicit val releaseFormat = jsonFormat11(Release.apply)
  implicit val phaseFormat = jsonFormat5(Phase.apply)
  implicit val taskFormat = jsonFormat5(Task.apply)
  implicit val scriptTaskFormat = jsonFormat6(ScriptTask.apply)
  implicit val dependencyFormat = jsonFormat3(Dependency.apply)
  implicit val specialDayFormat = jsonFormat5(SpecialDay.apply)
  implicit val directoryFormat = jsonFormat2(Directory.apply)
  implicit val commentFormat = jsonFormat3(Comment.apply)
  implicit val userFormat = jsonFormat5(User)
  implicit val roleFormat = jsonFormat2(Role)
  implicit val puserFormat = jsonFormat2(PUser)
  implicit val principalFormat = jsonFormat2(Principal)
  implicit val permissionFormat = jsonFormat2(Permission)
  implicit val httpConnectionFormat = jsonFormat3(HttpConnection.apply)
  implicit val attachmentFormat = jsonFormat3(Attachment.apply)
  implicit val activityLogEntryFormat = jsonFormat6(ActivityLogEntry.apply)
  implicit val folderFormat = jsonFormat3(Folder.apply)
  implicit val teamFormat = jsonFormat5(Team.apply)
  implicit val releaseTriggerFormat = jsonFormat8(ReleaseTrigger.apply)

  implicit object CiProtocol extends RootJsonFormat[Ci] {
    def read(json: JsValue): Ci = {
      deserializationError("Read is not implemented")
    }

    def write(ci: Ci): JsValue = {
      ci match {
        case ci: Release => ci.toJson
        case ci: Phase => ci.toJson
        case ci: Task => ci.toJson
        case ci: ScriptTask => ci.toJson
        case ci: Dependency => ci.toJson
        case ci: SpecialDay => ci.toJson
        case ci: Directory => ci.toJson
        case ci: Comment => ci.toJson
        case ci: HttpConnection => ci.toJson
        case ci: Attachment => ci.toJson
        case ci: ActivityLogEntry => ci.toJson
        case ci: Folder => ci.toJson
        case ci: Team => ci.toJson
        case ci: ReleaseTrigger => ci.toJson
        case _ => serializationError(s"Undefined CI type ${ci.getClass}")
      }
    }
  }
}

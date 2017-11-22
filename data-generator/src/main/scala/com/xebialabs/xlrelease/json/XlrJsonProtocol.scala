package com.xebialabs.xlrelease.json

import com.xebialabs.xlrelease.domain._
import spray.json._

trait XlrJsonProtocol extends DefaultJsonProtocol with AdditionalFormats with ZonedDateTimeProtocol {
  this: ProductFormatsInstances =>

  /**
    * The order of implicit definitions in this class matters!
    * First nested domain classes, then parent classes; e.g. first Phase then Release.
    * Otherwise you get a cryptic NullPointerException from `PimpedAny.toJson`.
    */

  implicit val commentFormat: RootJsonFormat[Comment] = jsonFormat3(Comment.apply)
  implicit val dependencyFormat: RootJsonFormat[Dependency] = jsonFormat3(Dependency.apply)

  implicit val taskFormat: RootJsonFormat[Task] = jsonFormat6(Task.apply)
  implicit val scriptTaskFormat: RootJsonFormat[ScriptTask] = jsonFormat7(ScriptTask.apply)
  implicit val gateTaskFormat: RootJsonFormat[GateTask] = jsonFormat7(GateTask.apply)

  implicit object AbstractTaskProtocol extends RootJsonFormat[AbstractTask] {
    def read(json: JsValue): AbstractTask = {
      deserializationError("Read is not implemented")
    }

    def write(task: AbstractTask): JsValue = task match {
      case t: Task => t.toJson
      case t: ScriptTask => t.toJson
      case t: GateTask => t.toJson
      case _ => serializationError(s"Undefined task type ${task.getClass}")
    }
  }

  implicit val phaseFormat: RootJsonFormat[Phase] = rootFormat(lazyFormat(jsonFormat6(Phase.apply)))
  implicit val attachmentFormat: RootJsonFormat[Attachment] = jsonFormat2(Attachment.apply)
  implicit val releaseTriggerFormat: RootJsonFormat[ReleaseTrigger] = jsonFormat8(ReleaseTrigger.apply)

  implicit val specialDayFormat: RootJsonFormat[SpecialDay] = jsonFormat4(SpecialDay.apply)
  implicit val riskProfileFormat: RootJsonFormat[RiskProfile] = jsonFormat5(RiskProfile.apply)
  implicit val directoryFormat: RootJsonFormat[Directory] = jsonFormat2(Directory.apply)
  implicit val userFormat: RootJsonFormat[User] = jsonFormat5(User)
  implicit val roleFormat: RootJsonFormat[Role] = jsonFormat2(Role)
  implicit val puserFormat: RootJsonFormat[PUser] = jsonFormat2(PUser)
  implicit val principalFormat: RootJsonFormat[Principal] = jsonFormat2(Principal)
  implicit val permissionFormat: RootJsonFormat[Permission] = jsonFormat2(Permission)
  implicit val httpConnectionFormat: RootJsonFormat[HttpConnection] = jsonFormat3(HttpConnection.apply)
  implicit val activityLogEntryFormat: RootJsonFormat[ActivityLogEntry] = jsonFormat6(ActivityLogEntry.apply)
  implicit val folderFormat: RootJsonFormat[Folder] = jsonFormat3(Folder.apply)
  implicit val teamFormat: RootJsonFormat[Team] = jsonFormat5(Team.apply)

  implicit object CiProtocol extends RootJsonFormat[Ci] {
    def read(json: JsValue): Ci = {
      deserializationError("Read is not implemented")
    }

    def write(ci: Ci): JsValue = {
      ci match {
        case ci: Release => ci.toJson
        case ci: Phase => ci.toJson
        case ci: AbstractTask => ci.toJson
        case ci: Dependency => ci.toJson
        case ci: SpecialDay => ci.toJson
        case ci: RiskProfile => ci.toJson
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

  implicit val releaseFormat: RootJsonFormat[Release] = jsonFormat15(Release.apply)

}

package com.xebialabs.xlrelease.stress.domain

import cats.Show
import spray.json._

sealed trait ReleaseStatus

object ReleaseStatus extends DefaultJsonProtocol {
  case object Template extends ReleaseStatus
  case object Planned extends ReleaseStatus
  case object InProgress extends ReleaseStatus
  case object Paused extends ReleaseStatus
  case object Completed extends ReleaseStatus
  case object Failing extends ReleaseStatus
  case object Failed extends ReleaseStatus
  case object Aborted extends ReleaseStatus


  implicit val showReleaseStatus: Show[ReleaseStatus] = {
    case Template => "TEMPLATE"
    case Planned => "PLANNED"
    case InProgress => "IN_PROGRESS"
    case Paused => "PAUSED"
    case Completed => "COMPLETED"
    case Failing => "FAILING"
    case Failed => "FAILED"
    case Aborted => "ABORTED"
  }

  implicit val releaseStatusReader: RootJsonReader[ReleaseStatus] = {
    case JsString(str) => str match {
      case "TEMPLATE" => Template
      case "PLANNED" => Planned
      case "IN_PROGRESS" => InProgress
      case "PAUSED" => Paused
      case "COMPLETED" => Completed
      case "FAILING" => Failing
      case "FAILED" => Failed
      case "ABORTED" => Aborted
      case unknown => deserializationError(s"Unknown release status: $unknown")
    }
    case _ => deserializationError("Wrong type for release status, expected JsString")
  }
  implicit val releaseStatusWriter: RootJsonWriter[ReleaseStatus] = ts => showReleaseStatus.show(ts).toJson

  implicit val releaseStatusFormat: RootJsonFormat[ReleaseStatus] = rootJsonFormat(releaseStatusReader, releaseStatusWriter)

}
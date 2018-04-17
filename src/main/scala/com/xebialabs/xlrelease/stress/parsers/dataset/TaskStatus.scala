package com.xebialabs.xlrelease.stress.parsers.dataset

import cats.Show
import spray.json._

sealed trait TaskStatus

object TaskStatus extends DefaultJsonProtocol {
  case object Planned extends TaskStatus
  case object Pending extends TaskStatus
  case object InProgress extends TaskStatus
  case object Completed extends TaskStatus
  case object Skipped extends TaskStatus
  case object Failed extends TaskStatus
  case object PreconditionInProgress extends TaskStatus
  case object Aborted extends TaskStatus
  case object WaitingForInput extends TaskStatus


  implicit val showTaskStatus: Show[TaskStatus] = {
    case Planned => "PLANNED"
    case Pending => "PENDING"
    case InProgress => "IN_PROGRESS"
    case Completed => "COMPLETED"
    case Skipped => "SKIPPED"
    case Failed => "FAILED"
    case PreconditionInProgress => "PRECONDITION_IN_PROGRESS"
    case Aborted => "ABORTED"
    case WaitingForInput => "WAITING_FOR_INPUT"
  }

  implicit val taskStatusReader: RootJsonReader[TaskStatus] = {
    case JsString(str) => str match {
      case "PLANNED" => Planned
      case "PENDING" => Pending
      case "IN_PROGRESS" => InProgress
      case "COMPLETED" => Completed
      case "SKIPPED" => Skipped
      case "FAILED" => Failed
      case "PRECONDITION_IN_PROGRESS" => PreconditionInProgress
      case "ABORTED" => Aborted
      case "WAITING_FOR_INPUT" => WaitingForInput
      case unknown => deserializationError(s"Unknown task status: $unknown")
    }
    case _ => deserializationError("Wrong type for task status, expected JsString")
  }
  implicit val taskStatusWriter: RootJsonWriter[TaskStatus] = ts => showTaskStatus.show(ts).toJson

  implicit val taskStatusFormat: RootJsonFormat[TaskStatus] = rootJsonFormat(taskStatusReader, taskStatusWriter)

}
package com.xebialabs.xlrelease.stress.parsers.dataset

import cats.Show

sealed trait TaskStatus

object TaskStatus {
  case object Pending extends TaskStatus
  case object InProgress extends TaskStatus
  case object Completed extends TaskStatus
  case object Skipped extends TaskStatus
  case object Failed extends TaskStatus
  case object PreconditionInProgress extends TaskStatus
  case object Aborted extends TaskStatus
  case object WaitingForInput extends TaskStatus


  implicit val showTaskStatus: Show[TaskStatus] = {
    case Pending => "PENDING"
    case InProgress => "IN_PROGRESS"
    case Completed => "COMPLETED"
    case Skipped => "SKIPPED"
    case Failed => "FAILED"
    case PreconditionInProgress => "PRECONDITION_IN_PROGRESS"
    case Aborted => "ABORTED"
    case WaitingForInput => "WAITING_FOR_INPUT"
  }

}
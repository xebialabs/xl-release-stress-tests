package com.xebialabs.xlrelease.stress.handlers.xlr.akkaClient

import cats.effect.IO
import com.xebialabs.xlrelease.stress.domain._
import spray.json.{JsArray, JsObject, JsString, JsValue}

trait jsUtils {

  def readTaskStatus: JsValue => Option[TaskStatus] = {
    case JsObject(fields) =>
      fields.get("status").map(_.convertTo[TaskStatus])
    case _ =>
      None
  }

  def readFirstTaskStatus: JsValue => Option[TaskStatus] = {
    case JsArray(arr) =>
      arr.headOption
        .flatMap(readTaskStatus)
    case _ => None
  }

  def matchesTaskStatus(expectedStatus: TaskStatus): JsValue => Boolean =
    readFirstTaskStatus andThen (_.contains(expectedStatus))

  def readReleaseStatus: JsValue => Option[ReleaseStatus] = {
    case JsObject(fields) =>
      fields.get("status").map(_.convertTo[ReleaseStatus])
    case _ => None
  }

  def matchesReleaseStatus(expectedStatus: ReleaseStatus): JsValue => Boolean = {
    case JsArray(arr) =>
      arr.headOption
        .flatMap(readReleaseStatus)
        .contains(expectedStatus)
    case _ => false
  }

  def getTaskId: String => Option[Task.ID] = {
    fullId => fullId.split("/").toList match {
      case _ :: _ :: Nil =>
        None
      case releaseId :: phaseId :: taskId =>
        Some(Task.ID(Phase.ID(releaseId, phaseId), taskId.mkString("/")))
      case _ =>
        None
    }
  }

  def getId: JsValue => Option[String] = {
    case JsObject(r) => r.get("id") match {
      case Some(JsString(id)) => Some(id.replaceFirst("Applications/", ""))
      case _ => None
    }
    case _ => None
  }

  def getIdIO: JsValue => IO[String] = js => getId(js).map(IO.pure).getOrElse(IO.raiseError(new RuntimeException("Cannot parse release id")))

}

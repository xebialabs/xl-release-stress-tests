package com.xebialabs.xlrelease.stress.utils

import cats.implicits._
import com.xebialabs.xlrelease.stress.domain._
import spray.json._

import scala.util.{Failure, Success, Try}

object JsUtils {
  type JsParsed[A] = Either[DeserializationException, A]

  def jsObject: JsValue => JsParsed[JsObject] = {
    case obj: JsObject => obj.asRight
    case other =>
      wrongType("jsObject", "JsObject", other)
  }

  def jsArray: JsValue => JsParsed[JsArray] = {
    case arr: JsArray => arr.asRight
    case other =>
      wrongType("jsArray", "JsArray", other)
  }

  def jsString: JsValue => JsParsed[JsString] = {
    case str: JsString => str.asRight
    case other =>
      wrongType("jsString", "JsString", other)
  }

  def jsBoolean: JsValue => JsParsed[JsBoolean] = {
    case b: JsBoolean => b.asRight
    case other =>
      wrongType("jsBoolean", "JsBoolean", other)
  }

  def getElements: JsValue => JsParsed[Seq[JsValue]] =
    json =>
      jsArray(json) map (_.elements)

  def getString: JsValue => JsParsed[String] =
    json =>
      jsString(json) map (_.value)

  def getFields: JsValue => JsParsed[Map[String, JsValue]] =
    json =>
      jsObject(json) map (_.fields)

  def getField(key: String): JsValue => JsParsed[JsValue] =
    json =>
      for {
        obj <- jsObject(json)
        res <- (obj.fields.get(key) map (_.asRight))
          .getOrElse(notFound("getField", key, obj))
      } yield res

  def getObjectField(key: String): JsValue => JsParsed[JsObject] =
    json =>
      for {
        field <- getField(key)(json)
        obj <- jsObject(field)
      } yield obj

  def getStringField(key: String): JsValue => JsParsed[JsString] =
    json =>
      for {
        field <- getField(key)(json)
        str <- jsString(field)
      } yield str

  def getArrayField(key: String): JsValue => JsParsed[JsArray] =
    json =>
      getField(key)(json) >>=
        jsArray

  def getBooleanField(key: String): JsValue => JsParsed[JsBoolean] =
    json =>
      getField(key)(json) >>=
        jsBoolean

  def getElements(key: String): JsValue => JsParsed[Seq[JsValue]] =
    json =>
      getField(key)(json) >>=
        getElements

  def getFirst: JsValue => JsParsed[JsValue] =
    json =>
      getFirstOption(json) >>= {
        case None =>
          err("getFirst: empty array.", json)
        case Some(first) =>
          first.asRight
      }

  def getFirstOption: JsValue => JsParsed[Option[JsValue]] =
    json =>
      jsArray(json) >>= { array =>
        array.elements.headOption.asRight
      }

  def readIdString: JsValue => JsParsed[String] =
    json =>
      getStringField("id")(json) map (_.value.replaceFirst("Applications/", ""))

  def readFirstId: JsValue => JsParsed[String] =
    json =>
      getFirst(json) >>=
        readIdString

  def readTaskId(sep: String): JsValue => JsParsed[Task.ID] =
    json =>
      readIdString(json) >>=
        parseTaskId(sep)

  def readTaskIds(sep: String): JsValue => JsParsed[List[Task.ID]] =
    json =>
      getElements(json) >>= readTaskIdsInner(sep)

  def readTaskIdsInner(sep: String): Seq[JsValue] => JsParsed[List[Task.ID]] =
    elements =>
      elements.toList
        .map(e => readIdString(e) >>= parseTaskId(sep))
        .sequence[JsParsed, Task.ID]

  def readDependencyId(sep: String): JsValue => JsParsed[Dependency.ID] =
    json =>
      readIdString(json) >>=
        parseDependencyId(sep)

  def getStatus: JsValue => JsParsed[JsValue] =
    getField("status")

  def readTaskStatus: JsValue => JsParsed[TaskStatus] =
    json =>
      getStatus(json) >>= toTaskStatus

  def readFirstTaskStatus: JsValue => JsParsed[Option[TaskStatus]] =
    json => {
      getFirstOption(json) >>= {
        case None =>
          Option.empty[TaskStatus].asRight
        case Some(first) =>
          readTaskStatus(first) match {
            case Left(_) =>
              Option.empty[TaskStatus].asRight
            case Right(status) =>
              Some(status).asRight
          }
      }
    }

  def matchesTaskStatus(expectedStatus: TaskStatus): JsValue => Boolean =
    json =>
      readFirstTaskStatus(json)
        .contains(expectedStatus)

  def readReleaseStatus: JsValue => JsParsed[ReleaseStatus] =
    json =>
      getStatus(json) >>= toReleaseStatus

  def readReleaseIdAndStatus: JsValue => JsParsed[(Release.ID, ReleaseStatus)] =
    json =>
      jsObject(json) >>= { obj =>
        for {
          id <- readIdString(obj)
          status <- readReleaseStatus(obj)
        } yield Release.ID(id) -> status
      }

  def readFirstPhaseId(sep: String): JsValue => JsParsed[Phase.ID] =
    json =>
      getField("phases")(json) >>=
        readFirstId >>=
        parsePhaseId(sep)

  def getTeamIdEntry: JsValue => JsParsed[(String, String)] =
    json =>
      jsObject(json) >>= { obj =>
        for {
          teamName <- getStringField("teamName")(obj)
          id <- getStringField("id")(obj)
        } yield teamName.value -> id.value
      }

  def readTeamIds: JsValue => JsParsed[Map[String, String]] =
    json =>
      (getElements(json) >>= readTeamIdsInner) map (_.toMap)

  def readTeamIdsInner: Seq[JsValue] => JsParsed[List[(String, String)]] =
    elements =>
      elements.toList
        .map(getTeamIdEntry)
        .sequence[JsParsed, (String, String)]

  def readTeams: JsValue => JsParsed[Seq[Team]] =
    json =>
      getElements(json) >>= readTeamsInner

  def readTeamsInner: Seq[JsValue] => JsParsed[List[Team]] =
    elements =>
      elements.toList
      .map(readTeam)
      .sequence[JsParsed, Team]

  def readTeam(implicit r: JsonReader[Team]): JsValue => JsParsed[Team] =
    json =>
      convert[Team](json)

  def readUsername: JsValue => JsParsed[User.ID] =
    json =>
      getStringField("username")(json) map (_.value)

  def readComment: JsValue => JsParsed[Comment] =
    json =>
      jsObject(json) >>= { obj =>
        getStringField("type")(obj) >>= {
          case JsString("xlrelease.Comment") =>
            for {
              id <- getStringField("id")(obj).map(_.value)
              author <- getStringField("author")(obj).map(_.value).orElse("".asRight)
              date <- getStringField("date")(obj).map(_.value)
              text <- getStringField("text")(obj).map(_.value)
            } yield Comment(id, author, date, text)
          case _ => wrongType("Not a comment", "type: xlrelease.Comment", obj)
        }
      }

  def readComments: JsValue => JsParsed[Seq[Comment]] =
    json =>
      getField("comments")(json) >>=
        getElements >>=
        (_.toList.map(readComment).sequence[JsParsed, Comment])

  def toTaskStatus: JsValue => JsParsed[TaskStatus] =
    json =>
      convert[TaskStatus](json)

  def toReleaseStatus: JsValue => JsParsed[ReleaseStatus] =
    json =>
      convert[ReleaseStatus](json)

  def parsePhaseId(sep: String = "/"): String => JsParsed[Phase.ID] =
    fullId =>
      fullId.split(sep).toList match {
        case releaseId :: phaseId :: Nil if isReleaseId(releaseId) && isPhaseId(phaseId) =>
          Phase.ID(Release.ID(releaseId), phaseId).asRight
        case _ =>
          parsePhaseIdError(fullId)
      }

  def parseTaskId(sep: String = "/"): String => JsParsed[Task.ID] =
    fullId =>
      fullId.split(sep).toList match {
        case _ :: _ :: Nil =>
          parseTaskIdError(fullId)
        case releaseId :: phaseId :: taskId if isReleaseId(releaseId) && isPhaseId(phaseId) && (taskId forall isTaskId) =>
          Task.ID(Phase.ID(Release.ID(releaseId), phaseId), taskId.mkString("/")).asRight
        case _ =>
          parseTaskIdError(fullId)
      }

  def parseDependencyId(sep: String = "/"): String => JsParsed[Dependency.ID] =
    fullId =>
      fullId.split(sep).toList match {
        case _ :: _ :: _ :: Nil =>
          parseDependencyIdError(fullId)
        case releaseId :: phaseId :: taskIdAndDependency =>
           taskIdAndDependency.span(_.startsWith("Task")) match {
             case (taskIds, dependencyId :: Nil) if taskIds.nonEmpty =>
               val phase = Phase.ID(Release.ID(releaseId), phaseId)
               val task = Task.ID(phase, taskIds.mkString("/"))
               Dependency.ID(task, dependencyId).asRight
             case _ => parseDependencyIdError(fullId)
           }
        case _ =>
          parseDependencyIdError(fullId)
      }

  def isReleaseId: String => Boolean = _.startsWith("Release")
  def isPhaseId: String => Boolean = _.startsWith("Phase")
  def isTaskId: String => Boolean = _.startsWith("Task")

  def parsePhaseIdError[A](fullId: String): JsParsed[A] =
    error(s"parsePhaseId: not a Phase ID: $fullId")

  def parseTaskIdError[A](fullId: String): JsParsed[A] =
    error(s"parseTaskId: not a Task ID: $fullId")

  def parseDependencyIdError[A](str: String): JsParsed[A] =
    error("parseDependencyId: not a Dependency ID: $fullId")

  def notFound[A](msg: String, key: String, actual: JsValue): JsParsed[A] =
    err(s"$msg: Not found, key: $key.", actual)

  def wrongType[A](msg: String, expected: String, actual: JsValue): JsParsed[A] =
    err(s"$msg: Wrong type: $jsType(actual), expected: $expected.", actual)

  def error[A](msg: String, original: Option[JsValue] = None, fieldNames: List[String] = Nil): JsParsed[A] =
    DeserializationException(
      msg,
      (original map debug(msg)).orNull,
      fieldNames
    ).asLeft

  def err[A](msg: String, original: JsValue, fieldNames: List[String] = Nil): JsParsed[A] =
    error(msg, Some(original), fieldNames)

  def debug(msg: String)(jsValue: JsValue): SerializationException =
    new SerializationException(msg + "\n" + jsValue.prettyPrint)

  def jsType[A <: JsValue]: JsValue => String = {
    case _: JsObject => "JsObject"
    case _: JsArray => "JsArray"
    case _: JsString => "JsString"
    case _: JsNumber => "JsNumber"
    case _: JsBoolean => "JsBoolean"
    case JsNull => "JsNull"
  }

  def convert[A](json: JsValue)(implicit reader: JsonReader[A]): JsParsed[A] =
    Try(json.convertTo[A]) match {
      case Failure(d: DeserializationException) => d.asLeft
      case Failure(err) => DeserializationException("JSON format error", err).asLeft
      case Success(a) => a.asRight
    }
}
package com.xebialabs.xlrelease.stress.dsl.libs.xlr

import akka.http.scaladsl.model.Uri
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.XlrServer
import com.xebialabs.xlrelease.stress.domain.Target._
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.dsl.DSL
import com.xebialabs.xlrelease.stress.utils.JsUtils
import freestyle.free._
import freestyle.free.implicits._
import spray.json._

import scala.concurrent.duration._
import scala.language.postfixOps


class Tasks[F[_]](server: XlrServer, phases: Phases[F])(implicit protected val _api: DSL[F]) extends XlrLib[F] with DefaultJsonProtocol {

  def append(task: JsObject, container: ConcreteTarget)
            (implicit session: User.Session): Program[Task.ID] =
    for {
      _ <- log.debug(s"xlr.tasks.append(${task.compactPrint}, ${container.show})")
      resp <- lib.http.json.post(server.api(_ ?/ "tasks" / "Applications" ++ container.path / "tasks"), task)
      taskId <- lib.json.parse(JsUtils.readTaskId(sep = "/"))(resp)
    } yield taskId

  def get(taskId: Task.ID)
         (implicit session: User.Session): Program[JsObject] =
    for {
      _ <- log.debug(s"xlr.tasks.get(${taskId.show})")
      content <- lib.http.json.get(server.api(_ ?/ "tasks" / "Applications" ++ taskId.path))
      task <- lib.json.read(JsUtils.jsObject)(content)
      _ <- lib.json.read(JsUtils.readTaskId(sep = "/"))(content)
    } yield task

  def delete(taskId: Task.ID)
            (implicit session: User.Session): Program[Unit] =
    for {
      _ <- log.debug(s"xlr.tasks.delete(${taskId.show})")
      resp <- lib.http.delete(server.api(_ ?/ "tasks" / "Applications" ++ taskId.path))
      _ <- api.http.discard(resp)
    } yield ()

  def copy(taskId: Task.ID, phaseId: Phase.ID, position: Int = 0)
          (implicit session: User.Session): Program[Task.ID] =
    for {
      _ <- log.debug(s"xlr.tasks.copy(${taskId.show}, ${phaseId.show})")
      resp <- lib.http.json.post(
        server.api(_ ?/ "tasks" / "Applications" ++ taskId.path / "copy").withQuery(Uri.Query(
            "targetContainerId" -> s"Applications/${phaseId.show}",
            "targetPosition" -> position.toString
        )),
        JsNull
      )
      newTaskId <- lib.json.parse(JsUtils.readTaskId(sep = "/"))(resp)
    } yield newTaskId

  def move(taskId: Task.ID, phaseId: Phase.ID)
          (implicit session: User.Session): Program[Task.ID] =
    for {
      newTaskId <- copy(taskId, phaseId)
      _ <- delete(taskId)
    } yield newTaskId

  def move(taskId: Task.ID, container: ConcreteTarget)
          (implicit session: User.Session): Program[Task.ID] = container match {
    case PhaseTarget(phaseId) => move(taskId, phaseId)
    case _ =>
      for {
        task <- get(taskId)
        newTaskId <- append(task, container)
        _ <- delete(taskId)
      } yield newTaskId
  }


  def move(taskId: Task.ID, subTaskId: Task.ID)
          (implicit session: User.Session): Program[Task.ID] =
    move(taskId, taskId.target)

  def appendScript(phaseId: Phase.ID, title: String, taskType: String, script: String)
                  (implicit session: User.Session): Program[Task.ID] =
    for {
      _ <- log.debug(s"xlr.tasks.appendScript(${phaseId.show}, $title, $taskType, $script)")
      resp <- lib.http.json.post(server.api(_ ?/ "tasks" / "Applications" / phaseId.release.id / phaseId.phase / "tasks"),
        JsObject(
          "id" -> JsNull,
          "title" -> title.toJson,
          "type" -> taskType.toJson,
          "script" -> script.toJson
        ))
      taskId <- lib.json.parse(JsUtils.readTaskId(sep = "/"))(resp)
    } yield taskId

  def appendManual(phaseId: Phase.ID, title: String)
                  (implicit session: User.Session): Program[Task.ID] =
    phases.appendTask(phaseId, title, "xlrelease.Task")

  def appendGate(phaseId: Phase.ID, title: String, targets: Seq[Target])
                (implicit session: User.Session): Program[(Task.ID, Seq[Dependency])] =
    for {
      taskId <- phases.appendTask(phaseId, title, "xlrelease.GateTask")
      dependencies <- targets.toList.map { target =>
        for (dependencyId <- addDependency(taskId, target))
          yield Dependency(dependencyId, target)
      }.sequence
    } yield taskId -> dependencies

  def addDependency(gateTaskId: Task.ID, target: Target)
                   (implicit session: User.Session): Program[Dependency.ID] =
    for {
      _ <- log.debug(s"xlr.tasks.addDependency(${gateTaskId.show}, ${target.show})")
      resp <- lib.http.json.post(server.api(_ ?/ "tasks" ++ gateTaskId.path / "dependencies" ++ gateTaskId.path),
        JsObject(
          "taskId" -> ("Applications/" ++ gateTaskId.show).toJson,
          "targetId" -> target.show.toJson
        )
      )
      dependencyId <- lib.json.parse(JsUtils.readDependencyId(sep = "/"))(resp)
    } yield dependencyId


  def assignTo(taskId: Task.ID, assignee: User.ID)
              (implicit session: User.Session): Program[Unit] =
    for {
      _ <- log.debug(s"xlr.tasks.assignTo(${taskId.show}, $assignee")
      resp <- lib.http.json.post(
        server.api(_ ?/ "tasks" / "Applications" ++ taskId.path / "assign" / assignee),
        JsNull
      )
      _ <- api.http.discard(resp)
    } yield ()

  def complete(taskId: Task.ID, comment: Option[String] = None)
              (implicit session: User.Session): Program[Boolean] =
    for {
      _ <- log.debug(s"xlr.tasks.complete(${taskId.show}, $comment)")
      resp <- lib.http.json.post(
        server.api(_ ?/ "tasks" / "Applications" ++ taskId.path / "complete"),
        comment.map(content => JsObject("comment" -> content.toJson)).getOrElse(JsObject.empty)
      )
      content <- api.http.parseJson(resp)
    } yield JsUtils.matchesTaskStatus(TaskStatus.Completed)(content)

  def fail(taskId: Task.ID, comment: String)
          (implicit session: User.Session): Program[Boolean] =
    for {
      _ <- log.debug(s"xlr.tasks.fail(${taskId.show}, $comment)")
      resp <- lib.http.json.post(
        server.api(_ ?/ "tasks" / "Applications" ++ taskId.path / "fail"),
        JsObject("comment" -> comment.toJson)
      )
      content <- api.http.parseJson(resp)
    } yield JsUtils.matchesTaskStatus(TaskStatus.Failed)(content)

  def retry(taskId: Task.ID, comment: String)
           (implicit session: User.Session): Program[Boolean] =
    for {
      _ <- log.debug(s"xlr.tasks.retry(${taskId.show}, $comment)")
      resp <- lib.http.json.post(
        server.api(_ ?/ "tasks" / "Applications" ++ taskId.path / "retry"),
        JsObject("comment" -> comment.toJson)
      )
      content <- api.http.parseJson(resp)
    } yield JsUtils.matchesTaskStatus(TaskStatus.InProgress)(content)

  def skip(taskId: Task.ID, comment: String)
          (implicit session: User.Session): Program[Comment.ID] = notImplemented("skip")

  def waitFor(taskId: Task.ID,
              expectedStatus: TaskStatus = TaskStatus.InProgress,
              interval: FiniteDuration = 5 seconds,
              retries: Option[Int] = Some(20))
             (implicit session: User.Session): Program[Unit] = {
    for {
      _ <- log.debug(s"xlr.tasks.waitFor(${taskId.show}, $expectedStatus, $interval, $retries")
      _ <- lib.control.until[TaskStatus](_ == expectedStatus, interval, retries) {
        getTaskStatus(taskId)
      }
    } yield ()

  }

  def getTaskStatus(taskId: Task.ID)
                   (implicit session: User.Session): Program[TaskStatus] =
    for {
      _ <- log.debug(s"xlr.tasks.getTaskStatus(${taskId.show})")
      resp <- lib.http.json.post(server.root(_ ?/ "tasks" / "poll"), JsObject("ids" -> Seq(taskId.show).toJson))
      taskStatus <- lib.json.parse(JsUtils.readFirstTaskStatus)(resp)
    } yield taskStatus


  def getComments(taskId: Task.ID)
                 (implicit session: User.Session): Program[Seq[Comment]] =
    for {
      _ <- log.debug(s"xlr.tasks.getComments(${taskId.show})")
      content <- lib.http.json.get(server.api(_ ?/ "tasks" / "Applications" ++ taskId.path))
      comments <- lib.json.read(JsUtils.readComments)(content)
    } yield comments

  private def notImplemented[A](name: String): Program[A] =
    api.error.error[A](new RuntimeException(s"Not implemented: $name"))
}

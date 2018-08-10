package com.xebialabs.xlrelease.stress.scenarios

import cats.Show
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.XlrConfig
import com.xebialabs.xlrelease.stress.domain.Target._
import com.xebialabs.xlrelease.stress.dsl.DSL
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.dsl.libs.xlr.protocol.CreateReleaseArgs
import freestyle.free._
import freestyle.free.implicits._
import spray.json._

import scala.concurrent.duration._
import scala.language.postfixOps


case class CommentsScenario(howMany: Int)
                           (implicit
                            val config: XlrConfig,
                            val _api: DSL[DSL.Op])
  extends Scenario[(Template.ID, Task.ID)]
    with ScenarioUtils
    with DefaultJsonProtocol {

  val name: String = s"Test Something"

  override def setup: Program[(Template.ID, Task.ID)] =
    api.xlr.users.admin() >>= { implicit session =>
      for {
        _ <- api.log.info("Creating parallel template...")
        phaseId <- api.xlr.templates.create("The ManyComments Template")
        par1 <- createParGroup(phaseId, "Par1", 24) { i =>
          JsObject(
            "id" -> JsNull,
            "type" -> "xlrelease.ScriptTask".toJson,
            "script" -> """""".toJson
          )
        }
        par1 <- createParGroup(phaseId, "Par2", 24) { i =>
          JsObject(
            "id" -> JsNull,
            "type" -> "xlrelease.ScriptTask".toJson,
            "script" -> """""".toJson
          )
        }
        controlId <- api.xlr.phases.appendTask(phaseId, "Control Task", "xlrelease.Task")
      } yield Template.ID(phaseId.release.id) -> controlId
    }

  protected def createParGroup(phaseId: Phase.ID, groupTitle: String, tasks: Int)
                              (mkTask: Int => JsObject)
                              (implicit session: User.Session): Program[(Task.ID, List[Task.ID])] =
    for {
      taskId <- api.xlr.phases.appendTask(phaseId, groupTitle, "xlrelease.ParallelGroup")
      container = taskId.target
      subTasks <- (1 to tasks).toList.map(mkTask).map { taskObj =>
        api.xlr.tasks.append(taskObj, container)
      }.sequence[Program, Task.ID]
    } yield taskId -> subTasks

  override def program(params: (Template.ID, Task.ID)): Program[Unit] =
    api.xlr.users.admin() >>= { implicit session =>
      val templateId = params._1
      rampUp(8, howMany, _ * 2) { _ =>
        api.control.repeat(5) {
          for {
            releaseId <- api.xlr.releases.createFromTemplate(templateId, CreateReleaseArgs("Test Release", Map.empty))
            taskId = params._2.copy(phaseId = params._2.phaseId.copy(release = releaseId))
            _ <- api.log.info(s"[${releaseId.show}] created")
            _ <- api.xlr.releases.start(releaseId)
            start <- api.control.now()
            _ <- api.log.info(s"[${releaseId.show}] started")
            end <- api.control.now()
            _ <- api.log.info(s"[${releaseId.show}] done in ${end.getMillis - start.getMillis}ms")
          } yield ()
        }.map(_ => ())
      }.map(_ => ())
    }

  override def cleanup(params: (Template.ID, Task.ID)): Program[Unit] = ().pure[Program]

  override implicit val showParams: Show[(Template.ID, Task.ID)] = { case (templateId, _) => templateId.show }
}

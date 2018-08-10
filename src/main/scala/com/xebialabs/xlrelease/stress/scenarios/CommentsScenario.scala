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
  extends Scenario[Template.ID]
    with ScenarioUtils
    with DefaultJsonProtocol {

  val name: String = s"Test Something"

  override def setup: Program[Template.ID] =
    api.xlr.users.admin() >>= { implicit session =>
      for {
        _ <- api.log.info("Creating parallel template...")
        phaseId <- api.xlr.templates.create("The ManyComments Template", scriptUser = Some(session.user))
        _ <- createParGroup(phaseId, "Par1", 4, 16)(i => myScriptTask(s"t1_$i"))
        _ <- createParGroup(phaseId, "Par2", 4, 16)(i => myScriptTask(s"t2_$i"))
        _ <- api.xlr.phases.appendTask(phaseId, "Control Task", "xlrelease.Task")
      } yield Template.ID(phaseId.release.id)
    }

  protected def parGroup(title: String): JsObject = JsObject(
    "id" -> JsNull,
    "type" -> "xlrelease.ParallelGroup".toJson,
    "title" -> title.toJson
  )

  protected def mkParGroup(container: ConcreteTarget, title: String, subTasks: Int)
                          (mkTask: Int => JsObject)
                          (implicit session: User.Session): Program[Task.ID] =
    for {
      p <- api.xlr.tasks.append(parGroup("p1"), container)
      _ <- (1 to subTasks).toList.map(mkTask).map { taskObj =>
        api.xlr.tasks.append(taskObj, p.target)
      }.sequence[Program, Task.ID].map(_ => ())
    } yield p

  protected def createParGroup(phaseId: Phase.ID, groupTitle: String, tasks: Int, subTasks: Int)
                              (mkTask: Int => JsObject)
                              (implicit session: User.Session): Program[Task.ID] =
    for {
      taskId <- api.xlr.phases.appendTask(phaseId, groupTitle, "xlrelease.ParallelGroup")
      _ <- (1 to tasks).toList.map(i => mkParGroup(taskId.target, s"p$i", subTasks)(mkTask)).sequence
    } yield taskId

  protected val script: String =
    s"""|import string
        |import random
        |import time
        |
        |def gen_comment(size=1024, chars=string.printable):
        |    return taskApi.newComment(''.join(random.choice(chars) for _ in range(size)))
        |
        |taskId = getCurrentTask().id
        |
        |def stress():
        |    comment = gen_comment(1024)
        |    taskApi.commentTask(taskId, comment)
        |    print(comment.comment + '\\n')
        |    return comment
        |
        |comment1 = stress()
        |comment2 = stress()
        |comment3 = stress()
        |comment4 = stress()
        |
        |print('DONE\\n')
       """.stripMargin

  protected def myScriptTask(title: String): JsObject = JsObject(
    "id" -> JsNull,
    "type" -> "xlrelease.ScriptTask".toJson,
    "title" -> title.toJson,
    "script" -> script.toJson
  )

  override def program(templateId: Template.ID): Program[Unit] =
    api.xlr.users.admin() >>= { implicit session =>
      rampUp(1, howMany, _ * 2) { _ =>
        api.control.repeat(2) {
          for {
            releaseId <- api.xlr.releases.createFromTemplate(templateId, CreateReleaseArgs("Test Release", Map.empty))
            par1 <- api.xlr.releases.getTasksByTitle(releaseId, "Par1").map(_.head)
            par2 <- api.xlr.releases.getTasksByTitle(releaseId, "Par2").map(_.head)
            ctrl <- api.xlr.releases.getTasksByTitle(releaseId, "Control Task").map(_.head)
            _ <- api.xlr.tasks.assignTo(ctrl, session.user.username)
            _ <- api.log.info(s"[${releaseId.show}] created")
            start <- api.control.now()
            _ <- api.xlr.releases.start(releaseId)
            _ <- api.log.info(s"[${releaseId.show}] started")
            _ <- api.xlr.tasks.waitFor(par1, TaskStatus.Completed, 5 seconds, None)
            _ <- api.log.info(s"[${par1.show}] Par1 completed")
            _ <- api.xlr.tasks.waitFor(par2, TaskStatus.Completed, 5 seconds, None)
            _ <- api.log.info(s"[${par2.show}] Par2 completed")
            _ <- api.xlr.tasks.waitFor(ctrl, TaskStatus.InProgress, 1 seconds, None)
            _ <- api.xlr.tasks.complete(ctrl, Some("COMPLETED"))
            _ <- api.log.info(s"[${ctrl.show}] Ctrl completed")
            end <- api.control.now()
            _ <- api.log.info(s"[${releaseId.show}] done in ${end.getMillis - start.getMillis}ms")
          } yield ()
        }.map(_ => ())
      }.map(_ => ())
    }

  override def cleanup(params: Template.ID): Program[Unit] = ().pure[Program]

  override implicit val showParams: Show[Template.ID] = Template.showTemplateId
}

package com.xebialabs.xlrelease.stress.scenarios

import cats._
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.AdminPassword
import com.xebialabs.xlrelease.stress.domain.{TaskStatus, Template}
import com.xebialabs.xlrelease.stress.dsl
import com.xebialabs.xlrelease.stress.dsl.Program
import freestyle.free._
import freestyle.free.implicits._

import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps
import scala.util.matching.Regex

case class CreateReleases(adminPassword: AdminPassword) extends Scenario[Unit] {
  override val name: String = s"Simple scenario to create release from groovy"

  val releasefile =
    Source.fromResource("DSL-template.groovy")
      .getLines()
      .mkString("\n")
      .replaceAll("###ADMIN_PASSWORD###", adminPassword.password)

  override def setup: Program[Unit] = api.log.info("nothing to do")

  val templateIdFromComment: Regex =
    "Created template \\[DSL\\]\\(#\\/templates\\/(.*)\\).".r

  def getTemplateId(str: String): Option[Template.ID] =
    templateIdFromComment.findFirstMatchIn(str).flatMap { m =>
      if (m.groupCount < 1) None else Some(m.group(1))
    }

  override def program(params: Unit): Program[Unit] =
    for {
      _ <- api.log.info("Starting scenario... logging in as admin")
      _ <- {
        api.xlr.users.admin() flatMap { implicit session =>
          for {
            phaseId <- api.xlr.releases.createRelease("test with releasefile")
            _ <- api.log.info(s"Release created: ${phaseId.release}")
            taskId <- api.xlr.tasks.appendScriptTask(phaseId, "dsl", "xlrelease.GroovyScriptTask", releasefile)
            _ <- api.log.info(s"Task created: ${taskId.task}")
            _ <- api.xlr.releases.start(phaseId.release)
            _ <- api.log.info("Started release...")
            _ <- api.xlr.tasks.waitFor(taskId, TaskStatus.Completed, interval = 5 seconds, retries = Some(20))
            _ <- api.log.info("Create template task completed")
            comments <- api.xlr.tasks.getComments(taskId)
            _ <- api.log.info(s"Task comments: ${comments.map(_.text).mkString("\n")}")
            comment <- comments.lastOption match {
              case None =>
                api.control.fail(s"No comments in task ${taskId.show}")
              case Some(comment) =>
                api.control.ok(comment)
            }
            templateId <- getTemplateId(comment.text) match {
              case Some(templateId) =>
                api.control.ok(templateId)
              case _ =>
                api.control.fail(s"Cannot extract templateId from comment: ${comment}")
            }
            _ <- api.log.info(s"Created template: $templateId")
          } yield ()
        }
      }
      _ <- api.log.info("End of scenario")
    } yield ()

  override def cleanup(params: Unit): Program[Unit] =
    for {
      _ <- api.log.info("nothing to do")
    } yield ()

  implicit val showParams: Show[Unit] = _ => ""

}

package com.xebialabs.xlrelease.stress.scenarios

import com.xebialabs.xlrelease.stress.domain.{Release, ReleaseStatus, TaskStatus, Template}
import com.xebialabs.xlrelease.stress.dsl.Program
import cats._
import cats.implicits._
import freestyle.free._
import freestyle.free.implicits._

import scala.util.matching.Regex
import scala.concurrent.duration._
import scala.language.postfixOps

trait ScenarioUtils { scenario: Scenario[_] =>
  import scenario.api

  protected val idFromCommentRegex: Regex =
    "Created (release|template) \\[.*\\]\\(#\\/(releases|templates)\\/(.*)\\).".r

  def getIdFromComment(str: String): Option[Template.ID] =
    idFromCommentRegex.findFirstMatchIn(str).flatMap { m =>
      if (m.groupCount < 3) {
        None
      } else {
        Some(m.group(3))
      }
    }

  def createReleaseFromGroovy(title: String,
                              groovyScript: String,
                              adminPasswordPlaceholder: String,
                              adminPassword: String): Program[Release.ID] = {
    val updated = groovyScript.replaceAll(adminPasswordPlaceholder, adminPassword)

    api.xlr.users.admin() flatMap { implicit session =>
      for {
        _ <- api.log.info(s"createReleaseFromGroovy($title)")
        phaseId <- api.xlr.releases.createRelease(title)
        _ <- api.log.debug(s"appendScriptTask(${phaseId.show}, groovy task, GroovScriptTask)")
        _ <- api.log.debug(updated)
        taskId <- api.xlr.tasks.appendScriptTask(phaseId, "groovy task", "xlrelease.GroovyScriptTask", updated)
        manualTaskId <- api.xlr.tasks.appendManualTask(phaseId, "wait before completing")
        _ <- api.xlr.tasks.assignTo(manualTaskId, session.user.username)
        _ <- api.log.debug(s"start(${phaseId.release})")
        _ <- api.xlr.releases.start(phaseId.release)
        _ <- api.log.debug(s"waitFor(${taskId.show}, completed, 5 seconds, 20 retries)")
        _ <- api.xlr.tasks.waitFor(taskId, TaskStatus.Completed, interval = 5 seconds, retries = Some(20))
        _ <- api.log.debug(s"getComments(${taskId.show})")
        comments <- api.xlr.tasks.getComments(taskId)
        _ <- api.log.debug(s"comments: ${comments.mkString("\n")}")
        comment <- comments.lastOption.map(api.control.ok).getOrElse {
          api.control.fail(s"No comments in task ${taskId.show}")
        }
        _ <- api.log.debug("last comment: "+ comment)
        releaseId <- getIdFromComment(comment.text).map(api.control.ok).getOrElse {
          api.control.fail(s"Cannot extract releaseId from comment: ${comment}")
        }
        _ <- api.xlr.tasks.complete(manualTaskId, comment = None)
        _ <- api.xlr.releases.waitFor(phaseId.release, status = ReleaseStatus.Completed, interval = 5 seconds, retries = Some(10))
        _ <- api.log.info(s"createReleaseFromGroovy(${title}) completed: ${releaseId}")
      } yield releaseId
    }
  }

}

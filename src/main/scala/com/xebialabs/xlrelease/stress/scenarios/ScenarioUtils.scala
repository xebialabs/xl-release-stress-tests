package com.xebialabs.xlrelease.stress.scenarios

import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.dsl
import cats._
import cats.implicits._
import freestyle.free._
import freestyle.free.implicits._

import scala.util.matching.Regex
import scala.concurrent.duration._
import scala.language.postfixOps

trait ScenarioUtils extends dsl.API {

  protected val idFromCommentRegex: Regex =
    "Created (release|template) \\[.*\\]\\(#\\/(releases|templates)\\/(.*)\\).".r

  protected def getIdFromComment(str: String): Option[Template.ID] =
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

    api.xlr.users.admin() >>= { implicit session =>
      for {
        _ <- api.log.debug(s"createReleaseFromGroovy($title)")
        phaseId <- api.xlr.releases.createRelease(title)
        taskId <- api.xlr.tasks.appendScriptTask(phaseId, "groovy task", "xlrelease.GroovyScriptTask", updated)
        manualTaskId <- api.xlr.tasks.appendManualTask(phaseId, "wait before completing")
        _ <- api.xlr.tasks.assignTo(manualTaskId, session.user.username)
        _ <- api.xlr.releases.start(phaseId.release)
        _ <- api.xlr.tasks.waitFor(taskId, TaskStatus.Completed, interval = 5 seconds, retries = Some(20))
        comments <- api.xlr.tasks.getComments(taskId)
        comment <- comments.lastOption.map(_.pure[Program]).getOrElse {
          api.fail(s"No comments in task ${taskId.show}")
        }
        _ <- api.log.debug("last comment: "+ comment)
        releaseId <- getIdFromComment(comment.text).map(_.pure[Program]).getOrElse {
          api.fail(s"Cannot extract releaseId from comment: $comment")
        }
        _ <- api.xlr.tasks.complete(manualTaskId, comment = None)
        _ <- api.xlr.releases.waitFor(phaseId.release, status = ReleaseStatus.Completed, interval = 5 seconds, retries = Some(10))
        _ <- api.log.info(s"createReleaseFromGroovy($title) completed: $releaseId")
      } yield releaseId
    }
  }
}

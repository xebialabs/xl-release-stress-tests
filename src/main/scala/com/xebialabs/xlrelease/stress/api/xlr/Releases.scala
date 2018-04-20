package com.xebialabs.xlrelease.stress.api.xlr

import com.xebialabs.xlrelease.stress.api.xlr.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.domain._
import freestyle.free._

import scala.concurrent.duration._
import scala.language.postfixOps

@free trait Releases {
  def importTemplate(template: Template)(implicit session: User.Session): FS[Template.ID]
  def setTemplateTeams(templateId: Template.ID, teams: Seq[Team])(implicit session: User.Session): FS[Map[String, String]]
  def setTemplateScriptUser(templateId: Template.ID, scriptUser: Option[User] = None)(implicit session: User.Session): FS[Unit]
//  def deleteTemplate(templateId: Template.ID): FS[Unit]

  def createFromTemplate(templateId: Template.ID, release: CreateReleaseArgs)(implicit session: User.Session): FS[Release.ID]
  def createRelease(title: String, scriptUser: Option[User] = None)(implicit session: User.Session): FS[Phase.ID]
  def start(releaseId: Release.ID)(implicit session: User.Session): FS[Release.ID]
  def getTasksByTitle(releaseId: Release.ID, taskTitle: String, phaseTitle: Option[String] = None)(implicit session: User.Session): FS[Set[Task.ID]]
//  def abortRelease(releaseId: Release.ID): FS[Unit]
//  def poll(releaseId: Release.ID): FS[Set[Task.ID]]
  def waitFor(releaseId: Release.ID, status: ReleaseStatus = ReleaseStatus.Completed,
              interval: Duration = 5 seconds, retries: Option[Int] = Some(20))
             (implicit session: User.Session): FS[Unit]

}

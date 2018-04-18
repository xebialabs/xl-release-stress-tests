package com.xebialabs.xlrelease.stress.client

import com.xebialabs.xlrelease.stress.client.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.domain._
import freestyle.free._

import scala.concurrent.duration._
import scala.language.postfixOps

@free trait Releases {
  def importTemplate(session: User.Session, template: Template): FS[Template.ID]
  def setTemplateTeams(session: User.Session, templateId: Template.ID, teams: Seq[Team]): FS[Map[String, String]]
  def setTemplateScriptUser(session: User.Session, templateId: Template.ID, scriptUser: Option[User] = None): FS[Unit]
//  def deleteTemplate(session: User.Session, templateId: Template.ID): FS[Unit]

  def create(session: User.Session, templateId: Template.ID, release: CreateReleaseArgs): FS[Release.ID]
  def start(session: User.Session, releaseId: Release.ID): FS[Release.ID]
  def getTasksByTitle(session: User.Session, releaseId: Release.ID, taskTitle: String, phaseTitle: Option[String] = None): FS[Set[Task.ID]]
//  def abortRelease(session: User.Session, releaseId: Release.ID): FS[Unit]
//  def poll(session: User.Session, releaseId: Release.ID): FS[Set[Task.ID]]
  def waitFor(session: User.Session, releaseId: Release.ID, status: ReleaseStatus = ReleaseStatus.Completed,
              interval: Duration = 5 seconds, retries: Option[Int] = Some(20)): FS[Unit]

}

package com.xebialabs.xlrelease.stress.client

import com.xebialabs.xlrelease.stress.client.protocol.CreateReleaseArgs
import com.xebialabs.xlrelease.stress.domain._
import freestyle.free._

@free trait Releases {
  def importTemplate(session: User.Session, owner: User, template: Template): FS[Template.ID]
  def setTemplateTeam(session: User.Session, templateId: Template.ID, teams: Set[Team]): FS[Map[String, String]]
  def setTemplateScriptUser(session: User.Session, templateId: Template.ID, scriptUser: Option[User] = None): FS[Unit]
//  def deleteTemplate(session: User.Session, templateId: Template.ID): FS[Unit]

  def create(session: User.Session, templateId: Template.ID, release: CreateReleaseArgs): FS[Release.ID]
  def start(session: User.Session, releaseId: Release.ID): FS[Release.ID]
  def getTasksByTitle(session: User.Session, releaseId: Release.ID, taskTitle: String, phaseTitle: Option[String] = None): FS[Set[Task.ID]]
//  def abortRelease(session: User.Session, releaseId: Release.ID): FS[Unit]
//  def poll(session: User.Session, releaseId: Release.ID): FS[Set[Task.ID]]
  def waitFor(session: User.Session, releaseId: Release.ID, status: ReleaseStatus = ReleaseStatus.Completed): FS[Unit]

}

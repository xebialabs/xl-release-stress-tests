package com.xebialabs.xlrelease.stress.client

import com.xebialabs.xlrelease.stress.parsers.dataset._
import freestyle.free._

@free trait Releases {
  def importTemplate(session: User.Session, owner: User, template: Template): FS[Template.ID]
  def setTemplateTeam(session: User.Session, templateId: Template.ID, teams: Set[Team]): FS[Map[String, String]]
//  def deleteTemplate(session: User.Session, templateId: Template.ID): FS[Unit]

  def createRelease(session: User.Session, templateId: Template.ID, release: CreateReleaseArgs): FS[Release.ID]
//  def startRelease(session: User.Session, releaseId: Release.ID): FS[Release.ID]
//  def abortRelease(session: User.Session, releaseId: Release.ID): FS[Unit]
}

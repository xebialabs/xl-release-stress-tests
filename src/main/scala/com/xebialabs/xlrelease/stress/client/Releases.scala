package com.xebialabs.xlrelease.stress.client

import com.xebialabs.xlrelease.stress.client.akkaClient.HttpSession
import com.xebialabs.xlrelease.stress.parsers.dataset.{CreateReleaseArgs, Release, Template, User}
import freestyle.free._

@free trait Releases {
  def importTemplate(session: HttpSession, template: Template): FS[Template.ID]
  def createRelease(session: HttpSession, templateId: Template.ID, release: CreateReleaseArgs): FS[Release.ID]
}

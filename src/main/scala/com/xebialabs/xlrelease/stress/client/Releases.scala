package com.xebialabs.xlrelease.stress.client

import com.xebialabs.xlrelease.stress.client.akkaClient.HttpSession
import com.xebialabs.xlrelease.stress.parsers.dataset.{Release, Template, User}
import freestyle.free._

@free trait Releases {
  def importTemplate(session: HttpSession, template: Template): FS[Template.ID]
  def createRelease(session: HttpSession, templateId: Template.ID): FS[Release.ID]
}

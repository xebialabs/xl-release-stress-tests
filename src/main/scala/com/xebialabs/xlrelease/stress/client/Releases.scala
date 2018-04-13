package com.xebialabs.xlrelease.stress.client

import com.xebialabs.xlrelease.stress.parsers.dataset.{Release, Template, User}
import freestyle.free._
import freestyle.free.implicits._

@free trait Releases {
  def importTemplate(session: User.Session, template: Template): FS[Template.ID]
  def createRelease(session: User.Session, templateId: Template.ID): FS[Release.ID]
}

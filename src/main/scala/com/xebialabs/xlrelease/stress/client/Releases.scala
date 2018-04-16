package com.xebialabs.xlrelease.stress.client

import com.xebialabs.xlrelease.stress.parsers.dataset.{CreateReleaseArgs, Release, Template, User}
import freestyle.free._

@free trait Releases {
  def importTemplate(session: User.Session, owner: User, template: Template): FS[Template.ID]
  def createRelease(session: User.Session, templateId: Template.ID, release: CreateReleaseArgs): FS[Release.ID]
}

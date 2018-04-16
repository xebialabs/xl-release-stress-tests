package com.xebialabs.xlrelease.stress.parsers.dataset

import java.nio.file.Path

import com.xebialabs.xlrelease.stress.parsers.dataset.Member.UserMember


case class Template(name: String, xlrTemplate: Path)

object Template {
  type ID = String

  implicit class TemplateOps(val template: Template) extends AnyVal {

  }

  implicit class TemplateIdOps(val templateId: Template.ID) extends AnyVal {
    def templateOwner(owner: User) = Team.templateOwner(owner, templateId)
    def releaseAdmin(owner: User) = Team.releaseAdmin(owner, templateId)
  }
}

package com.xebialabs.xlrelease.stress.domain

import java.io.File

import cats.Show


case class Template(name: String, xlrTemplate: File)

object Template {
  case class ID(id: String)

  implicit val showTemplateId: Show[Template.ID] = { case ID(id) => s"Applications/$id" }

}

package com.xebialabs.xlrelease.stress.domain

import java.nio.file.Path


case class Template(name: String, xlrTemplate: Path)

object Template {
  type ID = String
}

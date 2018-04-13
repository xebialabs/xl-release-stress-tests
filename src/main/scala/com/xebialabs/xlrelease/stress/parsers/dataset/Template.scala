package com.xebialabs.xlrelease.stress.parsers.dataset

import java.nio.file.Path


case class Template(name: String, xlrTemplate: Path)

object Template {
  type ID = String
}
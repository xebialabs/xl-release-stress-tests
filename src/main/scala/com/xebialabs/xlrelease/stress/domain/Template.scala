package com.xebialabs.xlrelease.stress.domain

import java.io.File


case class Template(name: String, xlrTemplate: File)

object Template {
  type ID = String
}

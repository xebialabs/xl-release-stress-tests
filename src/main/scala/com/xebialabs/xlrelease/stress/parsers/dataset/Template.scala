package com.xebialabs.xlrelease.stress.parsers.dataset

import java.io.File

case class Template(name: String, jsonTemplate: File) {

  println("file: "+ jsonTemplate + " "+ jsonTemplate.exists())
}

object Template {
  type ID = String
}
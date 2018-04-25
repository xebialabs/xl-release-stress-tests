package com.xebialabs.xlrelease.stress.domain

case class Folder(name: String, parent: Option[Folder], id: Folder.ID)

object Folder {
  type ID = String
}

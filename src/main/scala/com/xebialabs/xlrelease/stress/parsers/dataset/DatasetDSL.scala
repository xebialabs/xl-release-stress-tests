package com.xebialabs.xlrelease.stress.parsers.dataset

import java.io.File
import java.nio.file.Path


trait DatasetDSL {
  def users(name: String, template: User, num: Int): UsersSet =
    UsersSet(name, (0 to num).map(i =>
      template.copy(
        username = s"${template.username}$i",
        fullname = s"${template.fullname} $i",
        email = s"${template.username}$i@example.com",
        password = s"${template.username}$i"
      )).toList)

  def role(name: String, members: UsersSet): Role =
    Role(name, members)

  def template(name: String, xlrTemplate: Path): Template =
    Template(name, xlrTemplate)
}

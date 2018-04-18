package com.xebialabs.xlrelease.stress.parsers.dataset

import java.nio.file.Path

import com.xebialabs.xlrelease.stress.domain._


trait DatasetDSL {
  def user(username: User.ID, fullname: String, email: String, password: String): User =
    User(username, fullname, email, password)

  def user(username: String, password: String): User =
    User(username, "", "", password)

  def users(name: String, template: User, num: Int): UsersSet =
    UsersSet(name, (0 to num).map(i =>
      template.copy(
        username = s"${template.username}$i",
        fullname = s"${template.fullname} $i",
        email = s"${template.username}$i@example.com",
        password = s"${template.username}$i"
      )).toList)

  def role(name: String, permissions: Set[Permission], principals: Set[User.ID]): Role =
    Role(name, permissions, principals)

  def template(name: String, xlrTemplate: Path): Template =
    Template(name, xlrTemplate)
}


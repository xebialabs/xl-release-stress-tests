package com.xebialabs.xlrelease.generator

import com.xebialabs.xlrelease.domain.{Permission, Principal, Role, User, users2pusers}

class UsersAndRolesGenerator(emailDomain: String) {

  def generateUsers(amount: Int): Seq[User] = {
    val users = for {
      i <- 1 to amount
    } yield User(s"user$i", s"user$i", s"user$i@$emailDomain", s"User $i")

    val allViewer = User("viewer", "viewer", s"viewer@$emailDomain", "Viewer has access to folders")
    val noViewer = User("noViewer", "noViewer", s"noViewer@$emailDomain", "No Viewer user has no access to folders")

    users :+ allViewer :+ noViewer
  }

  def generateRoles(users: Seq[User]): Seq[Principal] = {
    val roles = users
      .filter(_.username.matches("user\\d+"))
      .zipWithIndex
      .map { case (user, i) =>
        Principal(Role(Some(100 + i), s"role${i + 1}"), users2pusers(Seq(user)))
      }
    val everybody = Principal(Role(name = "Everybody"), users)

    roles :+ everybody
  }

  def generatePermissions(roles: Seq[Principal]): Seq[Permission] =
    roles.map(r => Permission(r.role, Seq("reports#view")))

}

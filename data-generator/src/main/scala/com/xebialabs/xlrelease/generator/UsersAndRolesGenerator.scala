package com.xebialabs.xlrelease.generator

import com.xebialabs.xlrelease.domain.{Permission, Principal, Role, User, users2pusers}

class UsersAndRolesGenerator {

  def generateUsers(amount: Int): Seq[User] = {
    val users = for {
      i <- 1 to amount
    } yield User(s"user$i", s"user$i", "", s"User $i")

    val riskUsers = for {
      i <- 1 to amount
    } yield User(s"riskUser$i", s"riskUser$i", "", s"riskUser $i")

    val allRisk = User("risk", "risk", "", "Risk has access to edit Risk")
    val noRisk = User("norisk", "norisk", "", "No Risk user has no access to edit Risk")

    val allViewer = User("viewer", "viewer", "", "Viewer has access to folders")
    val noViewer = User("noViewer", "noViewer", "", "No Viewer user has no access to folders")

    users ++ riskUsers :+ allViewer :+ noViewer :+ allRisk :+ noRisk
  }

  def generateRoles(users: Seq[User]): Seq[Principal] = {
    val normalUsers = users
      .filter(_.username.matches("user\\d+"))
    val riskyusers = users
      .filter(_.username.matches("riskUser\\d+"))

    val roles = normalUsers
      .zipWithIndex
      .map { case (user, i) =>
        Principal(Role(Some(100 + i), s"role${i + 1}"), users2pusers(Seq(user)))
      }
    val everybody = Principal(Role(name = "Everybody"), normalUsers)

    val riskRoles = riskyusers
      .zipWithIndex
      .map { case (user, i) =>
        Principal(Role(Some(200 + i), s"riskRole${i + 1}"), users2pusers(Seq(user)))
      }
    val riskyUsers = Principal(Role(name = "RiskyUsers"), riskyusers)

    roles ++ riskRoles :+ riskyUsers :+ everybody
  }

  def generatePermissions(roles: Seq[Principal]): Seq[Permission] =
    roles.map(r => {
      if(r.role.name=="RiskyUsers"){
        Permission(Role(name = "RiskyUsers"), Seq("risk_profile#edit"))
      }else{
        Permission(r.role, Seq("reports#view"))
      }
    })

}

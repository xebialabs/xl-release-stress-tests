package com.xebialabs.xlrelease.stress.domain

import cats.Show

//case class Release(title: String, teams: Set[Team], phases: Seq[Phase])

object Release {
  case class ID(id: String)

  implicit val showReleaseId: Show[Release.ID] = { case ID(id) => s"Applications/$id" }
}


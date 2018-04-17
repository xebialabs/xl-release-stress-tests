package com.xebialabs.xlrelease.stress.parsers.dataset

import cats.Show

//case class Release(title: String, teams: Set[Team], phases: Seq[Phase])

object Release {
  type ID = String

  implicit val showReleaseId: Show[Release.ID] = releaseId => s"Applications/$releaseId"
}


package com.xebialabs.xlrelease.stress.parsers.dataset

//case class Phase(phaseId: Phase.ID, title: String, tasks: Seq[Task])

object Phase {
  case class ID(releaseId: Release.ID, phaseId: String)
}
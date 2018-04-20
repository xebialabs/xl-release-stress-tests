package com.xebialabs.xlrelease.stress.domain

import cats.Show

//case class Phase(phaseId: Phase.ID, title: String, tasks: Seq[Task])

object Phase {
  case class ID(release: Release.ID, phase: String)

  implicit def showPhaseId(implicit sr: Show[Release.ID]): Show[Phase.ID] = {
    case ID(releaseId, phaseId) => s"${sr.show(releaseId)}/$phaseId"
  }
}
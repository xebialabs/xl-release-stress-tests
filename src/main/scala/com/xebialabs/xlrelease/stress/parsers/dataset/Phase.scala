package com.xebialabs.xlrelease.stress.parsers.dataset

import cats.Show

//case class Phase(phaseId: Phase.ID, title: String, tasks: Seq[Task])

object Phase {
  case class ID(releaseId: Release.ID, phaseId: String)

  implicit def showPhaseId(implicit sr: Show[Release.ID]): Show[Phase.ID] = {
    case ID(releaseId, phaseId) => s"${sr.show(releaseId)}/$phaseId"
  }
}
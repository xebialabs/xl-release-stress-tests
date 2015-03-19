package com.xebialabs.xlrelease.generator

import com.xebialabs.xlrelease.domain.{Ci, Task, Phase, Release}

object ReleasesGenerator {

  val phasesPerRelease = 5
  val tasksPerPhase = 10

  def generateCompletedReleases(amount: Int): Seq[Seq[Ci]] = {
    val releases = (1 to amount).map( n => {
      Release.build(s"Applications/Release$n", s"Stress test release $n", "COMPLETED", n, amount)
    })

    releases.map( release => createReleaseContent(release) :+ release)
  }


  private def createReleaseContent(release: Release): Seq[Ci] = {
    val phases: Seq[Phase] = (1 to phasesPerRelease).map( n => Phase.build(s"Phase$n", release.id, "COMPLETED"))

    val tasks: Seq[Task] = phases.flatMap( phase => {
      (1 to tasksPerPhase).map( n => Task.build(s"Task$n", phase.id))
    })

    phases ++ tasks
  }


}

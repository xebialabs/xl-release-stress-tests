package com.xebialabs.xlrelease.generator

import java.util.concurrent.atomic.AtomicInteger

import com.xebialabs.xlrelease.domain.{Ci, Task, Phase, Release}

object ReleasesGenerator {

  val phasesPerRelease = 5
  val tasksPerPhase = 10
  val idCounter = new AtomicInteger()

  def generateCompletedReleases(amount: Int): Seq[Seq[Ci]] = {
    val releases = (1 to amount).map( n => {
      val releaseNumber = idCounter.incrementAndGet()
      Release.build(s"Applications/Release$releaseNumber", s"Stress test completed release $n", "COMPLETED", n, amount)
    })

    releases.map( release => createReleaseContent(release, "COMPLETED") :+ release)
  }

  def generateTemplateReleases(amount: Int): Seq[Seq[Ci]] = {
    val releases = (1 to amount).map( n => {
      val releaseNumber = idCounter.incrementAndGet()
      Release.build(s"Applications/Release$releaseNumber", s"Stress test template release $n", "TEMPLATE", n, amount)
    })

    releases.map( release => createReleaseContent(release, "PLANNED") :+ release)
  }

  def generateActiveReleases(amount: Int): Seq[Seq[Ci]] = {
    val releases = (1 to amount).map( n => {
      val releaseNumber = idCounter.incrementAndGet()
      Release.build(s"Applications/Release$releaseNumber", s"Stress test active release $n", "IN_PROGRESS", n, amount)
    })

    releases.map( release => createReleaseContent(release, "IN_PROGRESS") :+ release)
  }

  private def createReleaseContent(release: Release, status: String): Seq[Ci] = {
    val phases: Seq[Phase] = (1 to phasesPerRelease).map( n => Phase.build(s"Phase$n", release.id, status))

    val tasks: Seq[Task] = phases.flatMap( phase => {
      (1 to tasksPerPhase).map( n => Task.build(s"Task$n", phase.id, status = status))
    })

    phases ++ tasks
  }


}

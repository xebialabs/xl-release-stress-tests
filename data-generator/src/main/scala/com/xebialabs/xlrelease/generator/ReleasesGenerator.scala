package com.xebialabs.xlrelease.generator

import java.util.concurrent.atomic.AtomicInteger

import com.xebialabs.xlrelease.domain.{Ci, Task, Phase, Release}

object ReleasesGenerator {

  val phasesPerRelease = 5
  val tasksPerPhase = 10
  val idCounter = new AtomicInteger()

  def generateCompletedReleases(amount: Int): Seq[Seq[Ci]] = {
    generateReleases(amount, "COMPLETED", (n) => s"Stress test completed release $n")
  }

  def generateTemplateReleases(amount: Int): Seq[Seq[Ci]] = {
    generateReleases(amount, "TEMPLATE", (n) => s"Stress test template release $n")
  }

  def generateActiveReleases(amount: Int): Seq[Seq[Ci]] = {
    generateReleases(amount, "IN_PROGRESS", (n) => s"Stress test active release $n")
  }

  def generateReleases(amount: Int, status: String, titleGenerator: (Int) => String): Seq[Seq[Ci]] = {
    val releases = (1 to amount).map(n => {
      val releaseNumber = idCounter.incrementAndGet()
      Release.build(s"Applications/Release$releaseNumber", titleGenerator(n), status, n, amount)
    })

    releases.map(release => createReleaseContent(release) :+ release)
  }

  private def createReleaseContent(release: Release): Seq[Ci] = {
    val phases: Seq[Phase] = (1 to phasesPerRelease).map(
      n => Phase.build(s"Phase$n", release.id, phaseStatus(release)))

    val tasks: Seq[Task] = phases.flatMap( phase => {
      (1 to tasksPerPhase).map(
        n => Task.build(s"Task$n", phase.id, taskStatus(phase)))
    })

    phases ++ tasks
  }

  private def phaseStatus(release: Release): String = {
    release.status match {
      case "COMPLETED" => "COMPLETED"
      case "TEMPLATE" => "PLANNED"
      case "IN_PROGRESS" => "IN_PROGRESS"
    }
  }

  private def taskStatus(phase: Phase): String = {
    phase.status
  }
}

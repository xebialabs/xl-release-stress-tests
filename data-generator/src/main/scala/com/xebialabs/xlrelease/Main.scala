package com.xebialabs.xlrelease

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigFactory.parseResources
import com.typesafe.scalalogging.LazyLogging
import com.xebialabs.xlrelease.client.XlrClient
import com.xebialabs.xlrelease.generator.ReleasesGenerator
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Main extends App with LazyLogging {

  val config = parseResources("data-generator.conf")
      .withFallback(ConfigFactory.load())

  private val completedReleasesAmount = config.getInt("xl.data-generator.completed-releases")
  private val activeReleasesAmount = config.getInt("xl.data-generator.active-releases")
  private val templatesAmount: Int = config.getInt("xl.data-generator.templates")

  logger.info("Active releases: {}", activeReleasesAmount.toString)
  logger.info("Completed releases: {}", completedReleasesAmount.toString)
  logger.info("Templates: {}", templatesAmount.toString)

  val client = new XlrClient(
    config.getString("xl.data-generator.server-url"),
    config.getString("xl.data-generator.username"),
    config.getString("xl.data-generator.password"))

  val importTemplateFuture = client.importTemplate("/20-automated-tasks.xlr")
  
  val dependantReleaseFuture = client.createCis(ReleasesGenerator.generateDependentRelease())
  val allReleasesFuture = dependantReleaseFuture.flatMap(_ => {
    // Creating some content to increase repository size
    val createCompletedReleasesFutures = ReleasesGenerator
      .generateCompletedReleases(completedReleasesAmount)
      .map(client.createCis)
    val createTemplateReleasesFutures = ReleasesGenerator
      .generateTemplateReleases(templatesAmount)
      .map(client.createCis)
    val createActiveReleasesFutures = ReleasesGenerator
      .generateActiveReleases(activeReleasesAmount)
      .map(client.createCis)

    Future.sequence(
      createCompletedReleasesFutures ++
      createTemplateReleasesFutures ++
      createActiveReleasesFutures)
  })

  val allResponses = Future.sequence(Seq(importTemplateFuture, allReleasesFuture))

  allResponses.andThen {
    case _ =>
      client.system.shutdown()
      client.system.awaitTermination()
  }
}

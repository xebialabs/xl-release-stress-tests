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

  // The first 'Realistic' template which will be used by stress tests
  val importTemplateFuture = client.importTemplate("/20-automated-tasks.xlr")

  // Creating some content to increase repository size
  val createReleasesFutures = ReleasesGenerator.generateCompletedReleases(templatesAmount).map(client.createCis)

  val allResponses = Future.sequence(Seq(importTemplateFuture) ++ createReleasesFutures)

  allResponses.andThen {
    case _ =>
      client.system.shutdown()
      client.system.awaitTermination()
  }

}

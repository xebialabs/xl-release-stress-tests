package com.xebialabs.xlrelease

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigFactory.parseResources
import com.typesafe.scalalogging.LazyLogging
import com.xebialabs.xlrelease.client.XlrClient
import com.xebialabs.xlrelease.generator.{ReleasesGenerator, SpecialDayGenerator}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.sequence
import scala.util.Failure

object Main extends App with LazyLogging {

  implicit val config = parseResources("data-generator.conf")
    .withFallback(ConfigFactory.load())

  private val completedReleasesAmount = config.getInt("xl.data-generator.completed-releases")
  private val activeReleasesAmount = config.getInt("xl.data-generator.active-releases")
  private val templatesAmount = config.getInt("xl.data-generator.templates")
  private val createDepRels = config.getBoolean("xl.data-generator.create-dependency-releases")
  private val generateComments = config.getBoolean("xl.data-generator.generate-comments")
  private val foldersAmount = config.getInt("xl.data-generator.folders")
  private val foldersLevel = config.getInt("xl.data-generator.folders-level")

  logger.info("Active releases: {}", activeReleasesAmount.toString)
  logger.info("Completed releases: {}", completedReleasesAmount.toString)
  logger.info("Templates: {}", templatesAmount.toString)
  logger.info("Folders: {}", foldersAmount.toString)
  logger.info("Folder levels: {}", foldersLevel.toString)

  if (createDepRels) {
    logger.info("Creating {} releases with dependencies", completedReleasesAmount.toString)
  }
  if (generateComments) {
    logger.info("Generating releases with comments")
  }

  val client = new XlrClient(
    config.getString("xl.data-generator.baseUrl"),
    config.getString("xl.data-generator.username"),
    config.getString("xl.data-generator.password"))

  val importTemplateFuture = client.importTemplate("/many-automated-tasks.xlr")

  val specialDaysFuture = client.createOrUpdateCis(SpecialDayGenerator.generateSpecialDays())

  val releaseGenerator = new ReleasesGenerator()

  val (folders, activityLogs) = releaseGenerator.generateFoldersAndActivityLogs(foldersAmount, foldersLevel)
  val foldersFuture = client.createOrUpdateCis(folders ++ activityLogs)

  val allFoldersAndReleasesFuture = foldersFuture.flatMap(_ => {
    val dependantReleaseFuture = client.createOrUpdateCis(releaseGenerator.generateDependentRelease())

    dependantReleaseFuture.flatMap(_ => {
      // Creating some content to increase repository size

      val createTemplateReleasesFutures = releaseGenerator
        .generateTemplateReleases(templatesAmount, folders)
        .map(client.createCis)
      val createActiveReleasesFutures = releaseGenerator
        .generateActiveReleases(activeReleasesAmount, folders)
        .map(client.createCis)
      val (cis, completedIds) = releaseGenerator.generateCompletedReleases(completedReleasesAmount, folders, generateComments)
      val createCompletedReleasesFutures = cis.map(client.createCis)

      sequence(
        createTemplateReleasesFutures ++
          createActiveReleasesFutures ++
          createCompletedReleasesFutures
      ).map(f => f -> completedIds)
    })
  })

  val allFoldersAndReleasesWithDependencies = if (createDepRels) {
    allFoldersAndReleasesFuture.flatMap { case (f, ids) =>
      sequence(releaseGenerator.generateDepRelease(ids, completedReleasesAmount).map(client.createCis))
    }
  } else allFoldersAndReleasesFuture

  val allResponses = sequence(Seq(importTemplateFuture, allFoldersAndReleasesWithDependencies, specialDaysFuture))

  allResponses.andThen {
    case Failure(ex) =>
      logger.error("Could not generate data set: ", ex)
  } andThen {
    case _ =>
      logger.debug("Shutting down the actor system after everything has been done.")
      client.system.shutdown()
      client.system.awaitTermination()
  }
}

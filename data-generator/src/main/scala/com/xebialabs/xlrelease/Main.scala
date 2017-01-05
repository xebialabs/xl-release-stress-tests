package com.xebialabs.xlrelease

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigFactory.parseResources
import com.typesafe.scalalogging.LazyLogging
import com.xebialabs.xlrelease.client.XlrClient
import com.xebialabs.xlrelease.domain.User
import com.xebialabs.xlrelease.generator.{ReleasesAndFoldersGenerator, SpecialDayGenerator}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{sequence, successful}
import scala.util.Failure

object Main extends App with LazyLogging {

  implicit val config = parseResources("data-generator.conf")
    .withFallback(ConfigFactory.load())

  private val completedReleasesAmount = config.getInt("xl.data-generator.completed-releases")
  private val activeReleasesAmount = config.getInt("xl.data-generator.active-releases")
  private val templatesAmount = config.getInt("xl.data-generator.templates")
  private val automatedTemplatesAmount = config.getInt("xl.data-generator.automated-templates")
  private val createDepRels = config.getBoolean("xl.data-generator.create-dependency-releases")
  private val generateComments = config.getBoolean("xl.data-generator.generate-comments")
  private val foldersAmount = config.getInt("xl.data-generator.folders")
  private val foldersLevel = config.getInt("xl.data-generator.folders-level")

  private val dependencyTreeAmount = config.getInt("xl.data-generator.dependency-trees")
  private val dependencyTreeDepth = config.getInt("xl.data-generator.dependency-tree-depth")
  private val dependencyTreeBreadth = config.getInt("xl.data-generator.dependency-tree-breadth")

  logger.info("Active releases: {}", activeReleasesAmount.toString)
  logger.info("Completed releases: {}", completedReleasesAmount.toString)
  logger.info("Templates: {}", templatesAmount.toString)
  logger.info("Automated templates: {}", automatedTemplatesAmount.toString)
  logger.info("Folders: {}", foldersAmount.toString)
  logger.info("Folder levels: {}", foldersLevel.toString)

  if (createDepRels) {
    logger.info("Creating {} releases with dependencies", completedReleasesAmount.toString)
  }
  if (generateComments) {
    logger.info("Generating releases with comments")
  }
  if (dependencyTreeAmount > 0) {
    logger.info(s"Dependency trees: $dependencyTreeAmount (depth $dependencyTreeDepth, breadth $dependencyTreeBreadth)")
  }

  val client = new XlrClient(
    config.getString("xl.data-generator.baseUrl"),
    config.getString("xl.data-generator.username"),
    config.getString("xl.data-generator.password"))

  val importTemplateFuture = client.importTemplate("/many-automated-tasks.xlr")

  val specialDaysFuture = client.createOrUpdateCis(SpecialDayGenerator.generateSpecialDays())
  val usersFuture = sequence(Seq(
    client.createUser(User("viewer", "viewer", "", "Viewer has access to folders")),
    client.createUser(User("noViewer", "noViewer", "", "No Viewer user has no access to folders")))
  )

  val releaseGenerator = new ReleasesAndFoldersGenerator()

  val foldersAndRelatedCis = releaseGenerator.generateFolders(foldersAmount, foldersLevel)
  val foldersFuture = client.createOrUpdateCis(foldersAndRelatedCis)

  val allFoldersAndReleasesFuture = foldersFuture.flatMap(_ => {
    val dependantReleaseFuture = client.createOrUpdateCis(releaseGenerator.generateDependentRelease())

    dependantReleaseFuture.flatMap(_ => {

      val createTemplateReleasesFutures = releaseGenerator
        .generateTemplateReleases(templatesAmount)
        .map(client.createCis)

      val createAutomatedTemplatesFutures = releaseGenerator
        .generateAutomatedTemplates(automatedTemplatesAmount)
        .map(client.createCis)

      val createActiveReleasesFutures = releaseGenerator
        .generateActiveReleases(activeReleasesAmount)
        .map(client.createCis)

      val (cis, completedIds) = releaseGenerator.generateCompletedReleases(completedReleasesAmount, generateComments)

      val createCompletedReleasesFutures = cis.map(client.createCis)

      sequence(
        createTemplateReleasesFutures ++
          createAutomatedTemplatesFutures ++
          createActiveReleasesFutures ++
          createCompletedReleasesFutures
      ).map(f => f -> completedIds)
    })
  })

  val allFoldersAndReleasesWithDependencies = if (createDepRels) {
    allFoldersAndReleasesFuture.flatMap { case (f, ids) =>
      sequence(releaseGenerator.generateDepRelease(ids, completedReleasesAmount).map(client.createCis))
    }
  } else {
    allFoldersAndReleasesFuture
  }

  val allWithDependencyTrees = if (dependencyTreeAmount > 0) {
    allFoldersAndReleasesWithDependencies.flatMap(_ => {
      sequential(releaseGenerator.generateDependencyTrees(dependencyTreeAmount, dependencyTreeDepth, dependencyTreeBreadth)) {
        client.createCis
      }
    })
  } else {
    allFoldersAndReleasesWithDependencies
  }

  val allResponses = sequence(Seq(importTemplateFuture, allWithDependencyTrees, specialDaysFuture, usersFuture))

  allResponses.andThen {
    case Failure(ex) =>
      logger.error("Could not generate data set: ", ex)
  } andThen {
    case _ =>
      logger.debug("Shutting down the actor system after everything has been done.")
      client.system.shutdown()
      client.system.awaitTermination()
  }

  def sequential[T, U](items: TraversableOnce[T])(fn: T => Future[U]): Future[List[U]] = {
    items.foldLeft(successful[List[U]](Nil)) {
      (f, item) =>
        f.flatMap {
          x => fn(item).map(_ :: x)
        }
    } map (_.reverse)
  }
}

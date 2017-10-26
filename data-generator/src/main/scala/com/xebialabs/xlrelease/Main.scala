package com.xebialabs.xlrelease

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.config.ConfigFactory.parseResources
import com.typesafe.scalalogging.LazyLogging
import com.xebialabs.xlrelease.client.XlrClient
import com.xebialabs.xlrelease.generator.{ReleasesAndFoldersGenerator, SpecialDayGenerator, UsersAndRolesGenerator}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.{sequence, successful}
import scala.util.Failure

object Main extends App with LazyLogging {

  implicit val config: Config = parseResources("data-generator.conf")
    .withFallback(ConfigFactory.load())

  private val plannedReleasesAmount = config.getInt("xl.data-generator.planned-releases")
  private val activeReleasesAmount = config.getInt("xl.data-generator.active-releases")
  private val completedReleasesAmount = config.getInt("xl.data-generator.completed-releases")
  private val templatesAmount = config.getInt("xl.data-generator.templates")
  private val automatedTemplatesAmount = config.getInt("xl.data-generator.automated-templates")
  private val createDependencyReleases = config.getBoolean("xl.data-generator.create-dependency-releases")
  private val generateComments = config.getBoolean("xl.data-generator.generate-comments")
  private val foldersAmount = config.getInt("xl.data-generator.folders")
  private val foldersLevel = config.getInt("xl.data-generator.folders-level")

  private val dependencyTreeAmount = config.getInt("xl.data-generator.dependency-trees")
  private val dependencyTreeDepth = config.getInt("xl.data-generator.dependency-tree-depth")
  private val dependencyTreeBreadth = config.getInt("xl.data-generator.dependency-tree-breadth")

  logger.info("Planned releases: {}", plannedReleasesAmount.toString)
  logger.info("Active releases: {}", activeReleasesAmount.toString)
  logger.info("Completed releases: {}", completedReleasesAmount.toString)
  logger.info("Templates: {}", templatesAmount.toString)
  logger.info("Automated templates: {}", automatedTemplatesAmount.toString)
  logger.info("Folders: {}", foldersAmount.toString)
  logger.info("Folder levels: {}", foldersLevel.toString)

  if (createDependencyReleases) {
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

  val specialDaysFuture = client.createOrUpdateCis(SpecialDayGenerator.generateSpecialDays())

  private val usersAndRolesGenerator = new UsersAndRolesGenerator()
  val users = usersAndRolesGenerator.generateUsers(foldersAmount)
  val roles = usersAndRolesGenerator.generateRoles(users)
  val permissions = usersAndRolesGenerator.generatePermissions(roles)

  val usersAndRolesFuture =
    sequential(users)(client.createUser).flatMap { _ =>
      client.setRoles(roles)
    }.flatMap { _ =>
      client.setPermissions(permissions)
    }

  val usersAndRolesAndImportedTemplateFuture = usersAndRolesFuture.flatMap(_ =>
    client.importTemplate("/many-automated-tasks.xlr"))

  val releaseGenerator = new ReleasesAndFoldersGenerator()

  val foldersAndTeams = releaseGenerator.generateFolders(foldersAmount, foldersLevel)
  val teamsFuture = client.createFolders(foldersAndTeams._1).
    flatMap(_ => client.createTeams(foldersAndTeams._2))

  val allFoldersAndReleasesFuture = teamsFuture.flatMap(_ => {
    val dependantReleaseFuture = client.createRelease(releaseGenerator.generateDependentRelease())

    dependantReleaseFuture.flatMap(_ => {

      val createPlannedReleases = releaseGenerator
        .generatePlannedReleases(plannedReleasesAmount, generateComments)
        .map(client.createReleaseAndRelatedCis)

      val createTemplateReleasesFutures = releaseGenerator
        .generateTemplateReleases(templatesAmount, generateComments)
        .map(client.createReleaseAndRelatedCis)

      val createAutomatedTemplatesFutures = releaseGenerator
        .generateAutomatedTemplates(automatedTemplatesAmount, generateComments)
        .map(client.createReleaseAndRelatedCis)

      val createActiveReleasesFutures = releaseGenerator
        .generateActiveReleases(activeReleasesAmount, generateComments)
        .map(client.createReleaseAndRelatedCis)

      val completedReleases = releaseGenerator.generateCompletedReleases(completedReleasesAmount, generateComments)
      val completedReleaseIds = completedReleases.map(_.release.id)

      val createCompletedReleasesFutures = completedReleases.map(client.createReleaseAndRelatedCis)

      sequence(
        createPlannedReleases ++
          createTemplateReleasesFutures ++
          createAutomatedTemplatesFutures ++
          createActiveReleasesFutures ++
          createCompletedReleasesFutures
      ).map(f => f -> completedReleaseIds)
    })
  })

  val allFoldersAndReleasesWithDependencies = if (createDependencyReleases) {
    allFoldersAndReleasesFuture.flatMap { case (_, completedReleaseIds) =>
      sequence(
        releaseGenerator.generateReleasesDependingOn(completedReleaseIds, completedReleasesAmount)
          .map(client.createRelease)
      )
    }
  } else {
    allFoldersAndReleasesFuture
  }

  val allWithDependencyTrees = if (dependencyTreeAmount > 0) {
    allFoldersAndReleasesWithDependencies.flatMap(_ => {
      sequential(
        releaseGenerator.generateDependencyTrees(dependencyTreeAmount, dependencyTreeDepth, dependencyTreeBreadth)
      )(client.createReleaseAndRelatedCis)
    })
  } else {
    allFoldersAndReleasesWithDependencies
  }

  val allResponses = sequence(Seq(
    usersAndRolesAndImportedTemplateFuture,
    allWithDependencyTrees,
    specialDaysFuture
  ))

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

package com.xebialabs.xlrelease

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigFactory.parseResources
import com.typesafe.scalalogging.LazyLogging
import com.xebialabs.xlrelease.client.XlrClient
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App with LazyLogging {
  val config = parseResources("data-generator.conf")
      .withFallback(ConfigFactory.load())

  logger.info("Active releases: {}", config.getString("xl.data-generator.active-releases"))
  logger.info("Completed releases: {}", config.getString("xl.data-generator.completed-releases"))
  logger.info("Templates: {}", config.getString("xl.data-generator.templates"))

  val client = new XlrClient(
    config.getString("xl.data-generator.server-url"),
    config.getString("xl.data-generator.username"),
    config.getString("xl.data-generator.password"))
  val importTemplateFuture = client.importTemplate("/20-automated-tasks.xlr")

  importTemplateFuture.andThen {
    case _ =>
      client.system.shutdown()
      client.system.awaitTermination()
  }


}

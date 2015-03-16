package stress.utils

import com.typesafe.scalalogging.LazyLogging
import io.gatling.app.Gatling
import io.gatling.core.scenario.Simulation
import org.clapper.classutil.ClassFinder

/**
 * This runner allows to find and execute simulations from the same classpath as the runner itself.
 * Simplifies build logic.
 */
object GatlingRunner extends App with LazyLogging {

  private val simulationPropKey = "simulation"

  logger.info("Starting XL stress tests suite.")

  private val simulationProvValue = Option(System.getProperty(simulationPropKey))

  private val simulationClassOpt = simulationProvValue.map(Class.forName)

  val simulationsToRun = simulationClassOpt match {
    case Some(simulation) =>
      logger.info(s"Simulation has been specified explicitly: $simulationClassOpt")
      Seq(simulation)
    case None =>
      logger.info("Searching for all simulations on the classpath.")
      val classes = ClassFinder().getClasses().toIterator
      val simulations = ClassFinder.concreteSubclasses(classOf[Simulation].getName, classes).map({
        classInfo => Class.forName(classInfo.name)
      }).toList
      logger.info(s"Found following simulations to run: $simulations")
      simulations
  }


  simulationsToRun.foreach {
    case simulation =>
      logger.info(s"Starting simulation $simulation")
      Gatling.fromArgs(args, Some(simulation.asInstanceOf[Class[Simulation]])) match {
        case 0 =>
          logger.info(s"Finished execution of $simulation.")
        case e =>
          logger.info(s"Gatling returned non-zero exit code $e. Exiting.")
          sys.exit(e)
      }
  }

}

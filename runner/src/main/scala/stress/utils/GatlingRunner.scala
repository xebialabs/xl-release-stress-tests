package stress.utils

import io.gatling.app.Gatling
import io.gatling.core.scenario.Simulation

/**
 * This runner allows to run find and execute simulations from the same classpath as the runner itself.
 * Simplifies build logic.
 */
object GatlingRunner extends App {

  private val simulationPropKey = "simulation"

  private val simulationClassOpt = Option(System.getProperty(simulationPropKey))
    .map(Class.forName(_).asInstanceOf[Class[Simulation]])

  sys.exit(Gatling.fromArgs(args, simulationClassOpt))

}

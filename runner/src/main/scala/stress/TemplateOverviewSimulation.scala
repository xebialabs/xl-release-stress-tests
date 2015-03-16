package stress

import stress.utils.Scenarios._

import scala.language.postfixOps

/**
 * X users are looking at templates overview without filters
 */
class TemplateOverviewSimulation extends SimulationBase(queryTemplatesScenario)
package stress


import stress.utils.Scenarios

import scala.language.{implicitConversions, postfixOps}

/**
 * X users open Pipeline page
 */
class PipelineSimulation extends SimulationBase(Scenarios.queryPipelinesScenario)

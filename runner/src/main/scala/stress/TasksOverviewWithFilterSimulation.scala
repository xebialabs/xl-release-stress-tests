package stress

import io.gatling.core.Predef._
import io.gatling.http.request.StringBody
import stress.chain.Tasks

import scala.language.postfixOps

/**
 * X concurrent users are opening tasks overview page with a text filter which won't ever match
 */
class TasksOverviewWithFilterSimulation extends SimulationBase(

  scenario("X users are searching for not existing tasks")
    .exec(Tasks.tasks(
    StringBody("""{"active":true,"assignedToMe":true,"assignedToMyTeams":true,"assignedToOthers":true,"notAssigned":true,"filter":"non-existing"}""")
  ))

)
package stress.chain

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.http.request.Body

import scala.concurrent.duration.{Duration, _}
import scala.language.postfixOps
import scala.util.Random

object Tasks {

  val ALL_TASKS_FILTER = """{"active":false,"assignedToMe":true,"assignedToMyTeams":true,"assignedToOthers":true,"notAssigned":true,"filter":""}"""
  val MY_TASKS_FILTER = """{"active":false,"assignedToMe":true,"assignedToMyTeams":false,"assignedToOthers":false,"notAssigned":false,"filter":""}"""

  def open(filter: Body) = exec(
    http("Get list of tasks")
      .post("/tasks/search?limitTasksHint=100")
      .body(filter)
      .asJSON
      .check(
        jsonPath("$['releaseTasks'][*]['tasks'][*]['id']")
          .findAll
          .saveAs("taskIds")
      )
  )
  def open(filter: String): ChainBuilder = open(StringBody(filter))

  def openAndPoll(filter: String, pollDuration: Duration) = open(filter)
    .exec(session => {
      session.set("pollTasksBody", s"""{"ids":[${session.taskIds.map(s => s""""$s"""").mkString(",")}]}""")
    })
    .during(pollDuration) {
      exec(
        http("Poll tasks")
          .post("/tasks/poll")
          .body(StringBody("${pollTasksBody}"))
          .asJSON
      )
      .pause(2 seconds)
    }

  def commentOnRandomTask() = chooseRandomTask(ALL_TASKS_FILTER)
    .exec(
      http("Comment on a task")
        .post("/tasks/${randomTaskId}/comments")
        .body(StringBody("""{"text":"This task needs some comments"}"""))
        .asJSON
    )

  def changeTeamAssignmentOfRandomTask() = chooseRandomTask(ALL_TASKS_FILTER)
    .randomSwitch(
      50d -> setTaskTeam(Some("Release Admin")),
      50d -> setTaskTeam(None)
    )

  private def chooseRandomTask(filter: String) = open(filter)
    .exec(session => {
      val rnd = new Random()
      val taskId = if (session.taskIds.nonEmpty) session.taskIds.get(rnd.nextInt(session.taskIds.size)) else ""
      session.set("randomTaskId", taskId)
    })

  private def setTaskTeam(team: Option[String]) = {
    val teamJson = team match {
      case Some(t) => s""""$t""""
      case None => "null"
    }
    exec(
      http("Change task team assignment")
        .put("/tasks/${randomTaskId}/team")
        .body(StringBody( s"""{"team":$teamJson}"""))
        .asJSON
    )
  }

  private implicit class SessionEnhancedByTasks(session: Session) {
    def taskIds: Vector[String] = session.attributes.get("taskIds").get.asInstanceOf[Vector[String]]
  }

}

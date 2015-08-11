package stress.chain

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.http.request.Body
import stress.utils.TaskIds

import scala.concurrent.duration.{Duration, _}
import scala.language.postfixOps
import stress.config.RunnerConfig._

object Tasks {

  val ALL_TASKS_FILTER = """{"active":false,"assignedToMe":true,"assignedToMyTeams":true,"assignedToOthers":true,"notAssigned":true,"filter":""}"""
  val MY_TASKS_FILTER = """{"active":false,"assignedToMe":true,"assignedToMyTeams":false,"assignedToOthers":false,"notAssigned":false,"filter":""}"""
  val NOT_EXISTING_TASKS_FILTER = """{"active":false,"assignedToMe":true,"assignedToMyTeams":true,"assignedToOthers":true,"notAssigned":true,"filter":"non-existing"}"""

  def open(httpName: String, filter: Body) = exec(
    http(httpName)
      .post("/tasks/search?limitTasksHint=100")
      .body(filter)
      .asJSON
      .check(
        jsonPath("$['releaseTasks'][*]['tasks'][*]['id']")
          .findAll.optional
          .saveAs("taskIds")
      )
  )
  def open(httpName: String, filter: String): ChainBuilder = open(httpName, StringBody(filter))

  def pollManyTasks = exec(
    http("Poll tasks")
      .post("/tasks/poll")
      .body(StringBody(
      s"""{"ids":[${
        val tt = TaskIds.generate(20, 4, 4).map(t => s""""$t"""").mkString(", ")
        tt
      }]}""")).asJSON
  )

  def openAndPoll(httpName: String, filter: String, duration: Duration) = open(httpName, filter)
    .exec(session => {
      session.set("pollTasksBody", s"""{"ids":[${session.taskIds.map(s => s""""$s"""").mkString(",")}]}""")
    })
    .during(duration) {
      exec(
        http("Poll tasks")
          .post("/tasks/poll")
          .body(StringBody("${pollTasksBody}"))
          .asJSON
      )
      .pause(taskPollPause)
    }

  def commentOnRandomTask() = open("Get list of all tasks", ALL_TASKS_FILTER)
    .exec(
      http("Comment on a task")
        .post("/tasks/${taskIds.random()}/comments")
        .body(StringBody("""{"text":"This task needs some comments"}"""))
        .asJSON
    )

  def changeTeamAssignmentOfRandomTask() = open("Get list of all tasks", ALL_TASKS_FILTER)
    .randomSwitch(
      50d -> setTeamOnRandomTask(Some("Release Admin")),
      50d -> setTeamOnRandomTask(None)
    )

  private def setTeamOnRandomTask(team: Option[String]) = {
    val teamJson = team match {
      case Some(t) => s""""$t""""
      case None => "null"
    }
    exec(
      http("Change task team assignment")
        .put("/tasks/${taskIds.random()}/team")
        .body(StringBody(s"""{"team":$teamJson}"""))
        .asJSON
    )
  }

  private implicit class SessionEnhancedByTasks(session: Session) {
    def taskIds: Vector[String] = session("taskIds").asOption[Vector[String]].getOrElse(Vector())
  }

}

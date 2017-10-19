package stress.chain

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.http.request.Body
import stress.filters.TaskSearchFilter
import stress.utils.{Ids, TaskIds}

import scala.concurrent.duration.Duration
import scala.language.postfixOps

object Tasks {

  val ALL_TASKS_FILTER = TaskSearchFilter(assignedToMe = true, assignedToMyTeams = true, assignedToAnybody = true, notAssigned = true)
  val MY_TASKS_FILTER = TaskSearchFilter(assignedToMe = true)
  val NON_EXISTENT_TASKS_FILTER = TaskSearchFilter(
    assignedToMe = true,
    assignedToMyTeams = true,
    assignedToAnybody = true,
    notAssigned = true,
    filter = "non-existing")

  val TASK_IDS = "taskIds"
  val TASK_RELEASE_IDS = "taskReleaseIds"


  def open(httpName: String, filter: Body): ChainBuilder = exec(
    http(httpName)
      .post("/tasks/search?limitTasksHint=100")
      .body(filter)
      .asJSON
      .check(
        jsonPath("$['releaseTasks'][*]['tasks'][*]['id']")
          .findAll.optional
          .saveAs(TASK_IDS)
      )
      .check(
        jsonPath("$['releaseTasks'][*]['id']")
          .findAll.optional
          .saveAs(TASK_RELEASE_IDS)
      )
  )
  def open(httpName: String, filter: String): ChainBuilder = open(httpName, StringBody(filter))

  def pollManyTasks: ChainBuilder = exec(
    http("Poll tasks")
      .post("/tasks/poll")
      .body(StringBody(
      s"""{"ids":[${
        val tt = TaskIds.generate(20, 4, 4).map(t => s""""$t"""").mkString(", ")
        tt
      }]}""")).asJSON
  )

  def openAndPoll(httpName: String, filter: String, duration: Duration, taskPollPause: Duration): ChainBuilder = open(httpName, filter)
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

  def commentOnRandomTask: ChainBuilder = open("Get list of all tasks", ALL_TASKS_FILTER)
    .exec(
      http("Comment on a task")
        .post(s"/tasks/$${$TASK_IDS.random()}/comments")
        .body(StringBody("""{"text":"This task needs some comments"}"""))
        .asJSON
    )

  def changeTeamAssignmentOfRandomTask(): ChainBuilder = open("Get list of all tasks", ALL_TASKS_FILTER)
    .randomSwitch(
      50d -> setTeamOnRandomTask(Some("Release Admin")),
      50d -> setTeamOnRandomTask(None)
    )

  def commentOnTasks(): ChainBuilder =
    exec(session => {
      session.set("commentOnTasksBody",
        s"""{"$TASK_IDS":[${session.taskIds.map(taskId => s""""${Ids.toDomainId(taskId)}"""").mkString(",")}],
           |"commentText":"This task needs some comments"}""".stripMargin)
    })
    .exec(
      http("Comment on tasks")
        .post("/tasks/comments")
        .body(StringBody("${commentOnTasksBody}"))
        .asJSON
    )

  def changeAssignmentOnTasks(): ChainBuilder = exec(session => {
    session.set("changeAssignmentOnTasksBody",
        s"""{"$TASK_IDS":[${session.taskIds.map(taskId => s""""${Ids.toDomainId(taskId)}"""").mkString(",")}],
           |"team":"Release Admin", "owner": "admin"}""".stripMargin)
    })
    .exec(
      http("Change assignment on tasks")
        .put("/tasks/reassign")
        .body(StringBody("${changeAssignmentOnTasksBody}"))
        .asJSON
    )

  def removeTasks(): ChainBuilder = exec(session => {
    session.set("removeTasksBody",
      s"""[${session.taskIds.map(taskId => s""""${Ids.toDomainId(taskId)}"""").mkString(",")}]""".stripMargin)
  })
    .exec(
      http("Remove tasks")
        .delete("/tasks")
        .body(StringBody("${removeTasksBody}"))
        .asJSON
    )

  // In our generated dependency tree, the gate task is the last task
  def getDependencyCandidates: ChainBuilder =
    exec(http("Get gate task dependency candidates").get(s"/gates/$${${Releases.RELEASE_SESSION_ID}}-Phase5-Task10/dependency-target-candidates"))

  private def setTeamOnRandomTask(team: Option[String]) = {
    val teamJson = team match {
      case Some(t) => s""""$t""""
      case None => "null"
    }
    exec(
      http("Change task team assignment")
        .put(s"/tasks/$${$TASK_IDS.random()}/team")
        .body(StringBody(s"""{"team":$teamJson}"""))
        .asJSON
    )
  }

  private implicit class SessionEnhancedByTasks(session: Session) {
    def taskIds: Vector[String] = session(TASK_IDS).asOption[Vector[String]].getOrElse(Vector())
  }

}

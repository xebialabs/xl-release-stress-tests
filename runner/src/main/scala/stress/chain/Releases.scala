package stress.chain

import java.text.SimpleDateFormat
import java.util.Date

import com.ning.http.client.RequestBuilder
import io.gatling.core.Predef._
import io.gatling.core.config.GatlingConfiguration._
import io.gatling.core.config.Resource
import io.gatling.core.session._
import io.gatling.core.validation.Validation
import io.gatling.http.Predef._
import io.gatling.http.request.Body
import stress.config.RunnerConfig

object Releases {

  def create(body: Body) =
    exec(http("Get templates").get("/releases/templates?depth=1"))
      .exec(http("Post release").post("/releases").body(body).asJSON)
      .exec(http("Get tasks-definitions").get("/tasks/task-definitions"))

  def queryAll = exec(
    http("All releases")
      .post("/releases/search")
      .queryParam("numberbypage", RunnerConfig.queries.search.numberByPage)
      .queryParam("page", "0")
      .body(RawFileBody("release-search-body.json")).asJSON
  )

  def flow(release: String) =
    exec(http("Get polling interval").get("/settings/polling-interval"))
      .exec(http("Get reports").get("/settings/reports"))
      .exec(http("Get security").get("/security"))
      .exec(http("Get release").get(s"/releases/$release"))
      .exec(http("Get tasks-definitions").get("/tasks/task-definitions"))

  def createFromTemplate(jsonFilePath: String, templateTitle: String) =
    doIf(!_.attributes.isDefinedAt("releaseTemplateId")) {
      Templates.findTemplatesByTitle(templateTitle)
        .exec(session => {
          val ids = session("foundTemplateIds").as[Vector[String]]
          session.setAll(
            "releaseTemplateId" -> ids.head,
            "date" -> new SimpleDateFormat("YYYY-mm-dd").format(new Date()),
            "sshHost" -> RunnerConfig.input.sshHost,
            "sshUser" -> RunnerConfig.input.sshUser,
            "sshPassword" -> RunnerConfig.input.sshPassword
          )
        })
    }
    .exec(
        http("Post release")
          .post("/releases")
          .body(new ReplacingFileBody(jsonFilePath, Seq("releaseTemplateId", "date", "sshHost", "sshUser", "sshPassword")))
          .asJSON
          .check(
            jsonPath("$['id']")
              .find
              .saveAs("createdReleaseId")
          )
      )
    .exec(
        http("Start release")
          .post("/releases/${createdReleaseId}/start")
      )


  private class ReplacingFileBody(filePath: String, sessionAttributes: Seq[String]) extends Body {

    override def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = {
      val content: Validation[String] = Resource.body(filePath).map { _.string(configuration.core.charset) }
      val replacedContent = content.flatMap { contentString =>
        sessionAttributes.foldLeft(contentString) { (text, key) =>
          session(key).asOption[Any] match {
            case Some(value) => text.replaceAllLiterally("${" + key + "}", value.toString)
            case None => text
          }
        }
      }
      replacedContent.map(requestBuilder.setBody)
    }

  }

}

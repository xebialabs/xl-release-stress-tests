package stress.chain

import com.ning.http.client.RequestBuilder
import io.gatling.core.Predef._
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.Resource
import io.gatling.core.structure.ChainBuilder
import io.gatling.core.validation.Validation
import io.gatling.http.Predef._
import io.gatling.http.request.Body

import scala.util.Random


object Risk {
  val RISK_SESSION_ID = "risk_id"
  val NEW_RISK_PROFILE_ID = "new_risk_id"
  val DELETE_RISK_ID = "riskProfileID"

  def open: ChainBuilder = exec(
    http("Get all risks")
      .get("/api/v1/risks/profiles")
      .asJSON
      .check(
        jsonPath("$[*]['id']")
          .findAll
          .saveAs("riskIds")
      )
  )

  def create: ChainBuilder = exec(
    http("Create new risk Profile")
        .get("/api/v1/risks/profiles/new")
        .asJSON
  )
    .exec(session => {
      session.set("riskProfileID",null)
      session.set("riskProfileIndex", scala.util.Random.alphanumeric.take(4).mkString)
    })
    .exec(
      http("post new risk profile")
        .post("/api/v1/risks/profiles")
        .body(new ReplacingFileBody("update-risk-body.json",Seq("riskProfileID","riskProfileIndex")))
        .asJSON
        .check(jsonPath("$['id']").find.saveAs(DELETE_RISK_ID))
    )

  def delete: ChainBuilder = exec(
        http("delete risk Profile")
        .delete(s"/api/v1/risks/profiles/$${$DELETE_RISK_ID}")
        .check(status.is(204))
    )

  def edit: ChainBuilder = open
    .exec(session => {
      val riskIds = session("riskIds").asOption[Seq[String]].get takeRight 900
      val randomRiskId = (Random shuffle riskIds).headOption.get
      session.set(RISK_SESSION_ID, randomRiskId).set("riskProfileID", randomRiskId)
    })
      .exec(http("Get global config").get("/api/v1/risks/config").asJSON)
      .exec(
        http("Get risk profile").get(s"/api/v1/risks/profiles/$${$RISK_SESSION_ID}")
          .asJSON
          .check(jsonPath("$['title']").find.saveAs("riskProfileIndex")
          )
      )
      .exec(http("Get risk assessors").get("/api/v1/risks/assessors").asJSON)
      .exec(
        http("Update risk profile")
          .put(s"/api/v1/risks/profiles/$${$RISK_SESSION_ID}")
          .body(new ReplacingFileBody("update-risk-body.json",Seq("riskProfileID","riskProfileIndex")))
          .asJSON
      )
  
  private implicit class SessionEnhancedByRisks(session: Session) {
    def newRisk: String =  session(NEW_RISK_PROFILE_ID).asOption[String].getOrElse("")
  }

  private class ReplacingFileBody(filePath: String, sessionAttributes: Seq[String]) extends Body {

    override def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = {
      val content: Validation[String] = Resource.body(filePath).map {
        _.string(configuration.core.charset)
      }
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



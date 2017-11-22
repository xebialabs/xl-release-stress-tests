package stress.chain

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

import scala.util.Random


object Risk {
  val RISK_SESSION_ID = "risk_id"
  val NEW_RISK_PROFILE_ID = "new_risk_id"

  def open: ChainBuilder = exec(
    http("Get all risks")
      .get("api/v1/risks/profiles")
      .asJSON
      .check(
        jsonPath("$[*]['id']")
          .findAll
          .saveAs("riskIds")
      )
  )

  def create: ChainBuilder = exec(
    http("Create new risk Profile")
        .get("api/v1/risks/profiles/new")
        .asJSON
  )
    .exec(session => {
      session.set("riskProfileID",null)
      session.set("riskProfileIndex", Random.nextString(4))
    })
    .exec(
      http("post new risk profile")
        .post("api/v1/risks/profiles")
        .body(RawFileBody("update-risk-body"))
        .asJSON
    )

  def delete: ChainBuilder = exec(session => {
    session.set("riskProfileID",session.newRisk)
  })
    .exec(
        http("delete risk Profile")
        .delete("api/v1/risks/profiles/" + s"${"riskProfileID"}")
        .check(status.is(204))
    )

  def edit: ChainBuilder = open
    .exec(session => {
      val riskIds = session.get("riskIds").asOption[Seq[String]].get takeRight 900
      session.set(RISK_SESSION_ID, (Random shuffle riskIds).headOption.get)
      session.set("riskProfileID", RISK_SESSION_ID)
      session.set("riskProfileIndex", RISK_SESSION_ID)
    })
      .exec(http("Get global config").get("api/v1/risks/config").asJSON)
      .exec(
        http("Get risk profile").get("api/v1/risks/profiles/" + RISK_SESSION_ID)
          .asJSON
          .check(jsonPath("$.title").saveAs("riskProfileIndex")
          )
      )
      .exec(http("Get risk assessors").get("api/v1/risks/assessors").asJSON)
      .exec(
        http("Get risk profile")
          .put("api/v1/risks/profiles/" + RISK_SESSION_ID)
          .body(RawFileBody("update-risk-body"))
          .asJSON
      )
  
  private implicit class SessionEnhancedByRisks(session: Session) {
    def newRisk: String =  session(NEW_RISK_PROFILE_ID).asOption[String].getOrElse("")
  }
}



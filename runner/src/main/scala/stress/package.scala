import java.lang.Boolean

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}

package object stress {
  
  val nbUsers = Integer.getInteger("users", 10)
  val maxResponseSeconds = Integer.getInteger("maxResponseSeconds", 300)
  val baseUrl = Option(System.getProperty("baseUrl")).getOrElse("http://localhost:5516")
  val devMode = Boolean.parseBoolean(System.getProperty("devMode"))

  val nbTeams = Integer.getInteger("teams", 10)
  val nbOps = Integer.getInteger("ops", 20)
  val nbReleaseManagers = Integer.getInteger("releaseManagers", 20)

  val rampUpPeriod = if (devMode) 5 seconds else 1 minute

  val httpProtocol = http
    .baseURL(baseUrl)
    .acceptHeader("application/json")
    .authorizationHeader("Basic YWRtaW46YWRtaW4=")
    .contentTypeHeader("application/json; charset=UTF-8")

}

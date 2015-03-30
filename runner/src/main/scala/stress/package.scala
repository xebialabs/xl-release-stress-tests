import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.language.{implicitConversions, postfixOps}

package object stress {
  
  val nbUsers = Integer.getInteger("users", 10)
  val maxResponseSeconds = Integer.getInteger("maxResponseSeconds", 300)
  val baseUrl = Option(System.getProperty("baseUrl")).getOrElse("http://localhost:5516")

  val nbTeams = Integer.getInteger("teams", 10)
  val nbOps = Integer.getInteger("ops", 20)
  val nbReleaseManagers = Integer.getInteger("releaseManagers", 20)

  val username = Option(System.getProperty("username")).getOrElse("admin")
  val password = Option(System.getProperty("password")).getOrElse("admin")

  val httpProtocol = http
    .baseURL(baseUrl)
    .acceptHeader("application/json")
    .basicAuth(username, password)
    .contentTypeHeader("application/json; charset=UTF-8")

}

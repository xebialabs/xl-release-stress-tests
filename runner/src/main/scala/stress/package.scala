import com.typesafe.scalalogging.LazyLogging
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.language.{implicitConversions, postfixOps}

package object stress extends LazyLogging {

  val nbUsers = Integer.getInteger("users", 10)
  val maxResponseSeconds = Integer.getInteger("maxResponseSeconds", 300)

  private def getBaseUrls: Seq[String] = Option(System.getProperty("baseUrl")) match  {
    case None => Seq("http://localhost:5516")
    case Some(baseUrlString) => baseUrlString.split(",")
  }

  val nbTeams = Integer.getInteger("teams", 10)
  val nbOps = Integer.getInteger("ops", 20)
  val nbReleaseManagers = Integer.getInteger("releaseManagers", 20)

  val username = Option(System.getProperty("username")).getOrElse("admin")
  val password = Option(System.getProperty("password")).getOrElse("admin")

  val httpProtocol = http
      .baseURLs(getBaseUrls.toList)
      .acceptHeader("application/json")
      .basicAuth(username, password)
      .contentTypeHeader("application/json; charset=UTF-8").build
}

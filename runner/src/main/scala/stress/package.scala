import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.config.HttpProtocol
import stress.config.RunnerConfig

import scala.language.{implicitConversions, postfixOps}

package object stress {

  val httpProtocol: HttpProtocol = http
      .baseURLs(RunnerConfig.input.baseUrls)
      .acceptHeader("application/json")
      .basicAuth(RunnerConfig.input.username, RunnerConfig.input.password)
      .contentTypeHeader("application/json; charset=UTF-8").build
}

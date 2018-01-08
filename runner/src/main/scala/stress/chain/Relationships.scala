package stress.chain

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

object Relationships {

  import Releases.RELEASE_SESSION_ID

  def getRelationships: ChainBuilder =
    exec(http("All relationships").get(s"/relationships/graph/$${$RELEASE_SESSION_ID}"))

}

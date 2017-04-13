package stress.chain

import io.gatling.core.Predef.{exec, _}
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef.http

object PublicApi {

  def getTemplates: ChainBuilder = exec(
    http("Get all templates")
      .get("/api/v1/templates")
      .asJSON
  )

}

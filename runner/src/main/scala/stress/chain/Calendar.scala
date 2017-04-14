package stress.chain

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import org.joda.time.LocalDateTime


object Calendar {

  def open: ChainBuilder = exec(
    http("Get calendar releases")
      .post("/releases/search?depth=10&numberbypage=20")
      .body(StringBody(s"""{
            "active":true,
            "planned":true,
            "completed":false,
            "onlyMine":false,
            "onlyFlagged":false,
            "filter":"",
            "from":${new LocalDateTime(2014, 12, 28, 0, 0).toDate.getTime},
            "to":${new LocalDateTime(2015, 2, 7, 0, 0).toDate.getTime}
          }"""))
      .asJSON
      .check(
        jsonPath("$['cis'][*]['id']")
          .findAll
          .saveAs("releaseIds")
      ))
    .exec(
      http("Get special days for calendar")
        .get("/calendar/specialDays")
        .queryParam("from", "20141228")
        .queryParam("to", "20150207")
        .asJSON
    )


}

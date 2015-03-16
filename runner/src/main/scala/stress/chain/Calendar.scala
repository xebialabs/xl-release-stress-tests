package stress.chain

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.joda.time.LocalDateTime


object Calendar {

  def open = exec(
    http("Get calendar releases")
      .post("/releases/searchAll")
      .body(StringBody(s"""{
            "active":true,
            "planned":true,
            "completed":true,
            "onlyMine":false,
            "onlyFlagged":false,
            "filter":"",
            "from":${new LocalDateTime(2014, 12, 28, 0, 0).toDate.getTime},
            "to":${new LocalDateTime(2015, 2, 7, 0, 0).toDate.getTime}
          }"""))
      .asJSON
      .check(
        jsonPath("$[*]['id']")
          .findAll
          .saveAs("releaseIds")
      ))
    .execGetDependencies("calendar")
    .exec(
      http("Get special days for calendar")
        .get("/calendar/specialDays")
        .queryParam("from", "20141228")
        .queryParam("to", "20150207")
        .body(StringBody("${dependenciesBody}"))
        .asJSON
    )


}

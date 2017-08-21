package stress.chain

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import org.joda.time.LocalDateTime
import stress.filters.ReleaseSearchFilter


object Calendar {

  def open: ChainBuilder = exec(
    http("Get calendar releases")
      .post("/releases/search")
      .queryParam("numberbypage", 20)
      .queryParam("depth", 10)
      .body(StringBody(ReleaseSearchFilter(
        active = true,
        from = new LocalDateTime(2014, 12, 28, 0, 0).toDate,
        to = new LocalDateTime(2015, 2, 7, 0, 0).toDate
      )))
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

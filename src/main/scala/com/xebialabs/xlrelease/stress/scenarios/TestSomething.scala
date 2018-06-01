package com.xebialabs.xlrelease.stress.scenarios

import cats.Show
import cats.implicits._
import com.xebialabs.xlrelease.stress.config.XlrConfig
import com.xebialabs.xlrelease.stress.dsl.DSL
import com.xebialabs.xlrelease.stress.domain.Target._
import freestyle.free._
import freestyle.free.implicits._

import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.util.Random

case class TestSomething()
                        (implicit
                         val config: XlrConfig,
                         val _api: DSL[DSL.Op])
  extends Scenario[Unit]
    with ScenarioUtils {

  val name: String = s"Test Something"

  override def setup: Program[Unit] = ().pure[Program]

  override def program(params: Unit): Program[Unit] = {
    val dummy1 = for {
      _ <- api.log.info("DUMMY. waiting 20 seconds...")
      _ <- api.control.sleep(new FiniteDuration(20, SECONDS))
      _ <- api.log.info("DUMMY: DONE")
    } yield ()
    val bg: Program[Int] = for {
      _ <- api.log.info("BG: tick")
      _ <- api.control.sleep(new FiniteDuration(1, SECONDS))
    } yield Random.nextInt
    //      api.xlr.users.admin() >>= { implicit session =>
    //        for {
    //          results <- withHealthCheck(program = generateLoad, checkInterval = new FiniteDuration(1, SECONDS), checkProgram = checkReleases)
    //          (_, health) = results
    //          _ <- api.log.info("**** RESULTS *****")
    //          _ <- health.map { case (t, d, releasesByStatus) =>
    //            api.log.info(s"${t.toString} | ${d.toMillis}ms | ${releasesByStatus.toString}")
    //          }.sequence
    //        } yield ()
    //      }
    api.control.backgroundOf(dummy1)(bg).flatMap { case (_, checks) =>
      api.log.info("bgOf done: checks: " + checks.toString)
    }
  }
//    api.xlr.users.admin() >>= { implicit session =>
//      for {
//        phaseId <- api.xlr.releases.createRelease("Test Release XY", Some(session.user))
//        releaseId = phaseId.release
//        m1 <- api.xlr.tasks.appendManual(phaseId, "m1")
//        m2 <- api.xlr.tasks.appendManual(phaseId, "m2")
//        pg1 <- api.xlr.phases.appendTask(phaseId, "pg1", "xlrelease.ParallelGroup")
//        task1 <- api.xlr.tasks.get(m1)
//        task2 <- api.xlr.tasks.get(m2)
//        pg1m1 <- api.xlr.tasks.append(task1, pg1.target)
//        pg1m2 <- api.xlr.tasks.append(task2, pg1.target)
//        _ <- api.xlr.tasks.delete(m1)
//        _ <- api.xlr.tasks.delete(m2)
////        pg1m1 <- api.xlr.tasks.move(m1, pg1)
////        pg1m2 <- api.xlr.tasks.move(m2, pg1)
//        _ <- api.log.info(s"pg1: ${pg1.show}")
//        _ <- api.log.info(s"pg1m1: ${pg1m1.show}")
//        _ <- api.log.info(s"pg1m2: ${pg1m2.show}")
//      } yield ()
//    }


  override def cleanup(params: Unit): Program[Unit] = ().pure[Program]

  override implicit val showParams: Show[Unit] = _ => ""
}

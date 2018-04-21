package com.xebialabs.xlrelease.stress.http.handlers.future

import com.xebialabs.xlrelease.stress.http.{Client, Time}
import org.joda.time.{DateTime, Duration}

import scala.concurrent.{ExecutionContext, Future}

class TimeHandler()(implicit
                    handle: Client.Handler[Future],
                    ec: ExecutionContext) {

  implicit def timeHandler: Time.Handler[Future] = new Time.Handler[Future] {
    protected def now(): Future[DateTime] = Future.apply(DateTime.now)

    protected def time[A](op: Client.Op[A]): Future[(Duration, A)] =
      for {
        start <- now()
        result <- handle(op)
        end <- now()
        duration = new Duration(start, end)
      } yield duration -> result
  }
}

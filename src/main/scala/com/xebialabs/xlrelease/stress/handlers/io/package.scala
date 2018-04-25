package com.xebialabs.xlrelease.stress.handlers

import cats.effect.IO

import scala.concurrent.Future

package object io {

  implicit class ToIO[A](val future: Future[A]) extends AnyVal {
    def io: IO[A] = IO.fromFuture(IO(future))
  }

}

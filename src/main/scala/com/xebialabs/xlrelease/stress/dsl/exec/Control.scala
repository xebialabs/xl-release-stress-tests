package com.xebialabs.xlrelease.stress.dsl.exec

import com.xebialabs.xlrelease.stress.dsl.Program
import freestyle.free._

import scala.concurrent.duration.Duration

@free trait Control {
  def parallel[A](n: Int)(p: Int => Program[A]): FS[List[A]]

  def pause(duration: Duration): FS[Unit]

  def ok[A](value: A): FS[A]

  def error[A](error: Throwable): FS[A]

  def fail[A](msg: String): FS[A] =
    error[A](new RuntimeException(msg))
}

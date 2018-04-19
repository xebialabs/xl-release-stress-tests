package com.xebialabs.xlrelease.stress.engine

import com.xebialabs.xlrelease.stress.client.Program
import freestyle.free._

import scala.concurrent.duration.Duration

@free trait Engine {
  def parallel[A](n: Int)(p: Int => Program[A]): FS[List[A]]

  def pause(duration: Duration): FS[Unit]
}

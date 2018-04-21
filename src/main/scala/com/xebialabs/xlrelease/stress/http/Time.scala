package com.xebialabs.xlrelease.stress.http

import freestyle.free._
import org.joda.time.{DateTime, Duration}

@free trait Time {
  def now(): FS[DateTime]
  def time[A](op: Client.Op[A]): FS[(Duration, A)]
}

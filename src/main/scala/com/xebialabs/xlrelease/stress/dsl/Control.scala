package com.xebialabs.xlrelease.stress.dsl

import freestyle.free._
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration

@free trait Control {
  def sleep(duration: FiniteDuration): FS[Unit]

  def fork[A, B](left: Program[A], right: Program[B]): FS[(A, B)]

  def backgroundOf[A, B](foreground: Program[A])(background: Program[B]): FS[(A, List[B])]

  def repeat[A](n: Int)(p: Program[A]): FS[List[A]]

  def parallel[A](n: Int)(p: Int => Program[A]): FS[List[A]]

  def now(): FS[DateTime]

  def time[A](program: Program[A]): FS[(FiniteDuration, A)]
}

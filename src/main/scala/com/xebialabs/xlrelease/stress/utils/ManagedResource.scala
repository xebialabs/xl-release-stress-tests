package com.xebialabs.xlrelease.stress.utils

import java.io.Closeable

import scala.language.implicitConversions

trait ManagedResource[T] {
  def onEnter(): T
  def onExit(exceptionHappened: Boolean = false): Unit
  def attempt(block: => Unit): Unit = {
    try { block } finally {}
  }
}

class ManagedClosable[T <: Closeable](out: T) extends ManagedResource[T] {
  def onEnter(): T = out
  def onExit(exceptionHappened: Boolean): Unit = {
    attempt(out.close())
  }
}



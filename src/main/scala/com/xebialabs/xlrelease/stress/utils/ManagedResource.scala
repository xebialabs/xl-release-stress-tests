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

object ResourceManagement {
  def map[T <: Any, A <: Any](managed: ManagedResource[T])(body: T => A): A = {
    val resource = managed.onEnter()
    var exception = false
    try {
      body(resource)
    } catch {
      case e: Throwable =>
        exception = true
        managed.onExit(exceptionHappened = true)
        throw e
    } finally {
      if (!exception) managed.onExit()
    }
  }

  def using[T <: Any](managed: ManagedResource[T])(body: T => Unit) {
    map(managed)(body)
  }

  def using[T <: Any, U <: Any](managed1: ManagedResource[T], managed2: ManagedResource[U])(body: T => U => Unit) {
    using[T](managed1) { r: T =>
      using[U](managed2) { s: U => body(r)(s) }
    }
  }

  def using[T <: Closeable, U <: Any](closeable: T)(body: T => U): U = {
    try {
      body(closeable)
    } finally {
      closeable.close()
    }
  }

  implicit def closable2managed[T <: Closeable](out: T): ManagedResource[T] = new ManagedClosable(out)
}

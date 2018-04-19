package com.xebialabs.xlrelease.stress.engine

import com.xebialabs.xlrelease.stress.client.Program
import freestyle.free._

@free trait Engine {
  def par[A, B](p1: Program[A], p2: Program[B]): FS[(A, B)]
  def parallel[A](n: Int)(p: Int => Program[A]): FS[List[A]]
}

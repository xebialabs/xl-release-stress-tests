package com.xebialabs.xlrelease.stress.dsl.handlers.io.xlr

import akka.http.scaladsl.model.Uri
import com.xebialabs.xlrelease.stress.config.XlrServer

trait XlrRest {
  val server: XlrServer

  val apiPath: Uri.Path = server.uri.path / "api" / "v1"

  def root(path: Uri.Path => Uri.Path): Uri = server.uri.withPath(path(server.uri.path))

  def api(path: Uri.Path => Uri.Path): Uri = root(_ => path(apiPath))
}

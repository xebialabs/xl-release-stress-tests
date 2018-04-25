package com.xebialabs.xlrelease.stress.config

import akka.http.scaladsl.model.Uri

case class XlrServer(server: Uri) {

  lazy val rootPath: Uri.Path = server.path

  lazy val apiPath: Uri.Path = rootPath / "api" / "v1"

  def root(path: Uri.Path => Uri.Path): Uri = server.withPath(path(rootPath))

  def api(path: Uri.Path => Uri.Path): Uri = root(_ => path(apiPath))
}

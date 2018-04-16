package com.xebialabs.xlrelease.stress.client.akkaClient

import com.xebialabs.xlrelease.stress.client.Releases
import com.xebialabs.xlrelease.stress.parsers.dataset.{CreateReleaseArgs, Template}
import com.xebialabs.xlrelease.stress.parsers.dataset.Template.ID

import scala.concurrent.Future

class ReleasesHandler(val client: AkkaHttpXlrClient) {

  implicit def releasesHandler: Releases.Handler[Future] = new Releases.Handler[Future] {
    protected def importTemplate(session: HttpSession, template: Template): Future[ID] =
      client.importTemplate(template)(session)

    protected def createRelease(session: HttpSession, templateId: ID, createReleaseArgs: CreateReleaseArgs): Future[ID] =
      client.createRelease(templateId, createReleaseArgs)(session)
  }
}

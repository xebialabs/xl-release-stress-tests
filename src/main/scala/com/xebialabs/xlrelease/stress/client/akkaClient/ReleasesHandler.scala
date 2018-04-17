package com.xebialabs.xlrelease.stress.client.akkaClient

import com.xebialabs.xlrelease.stress.client.Releases
import com.xebialabs.xlrelease.stress.parsers.dataset.Team.{releaseAdmin, templateOwner}
import com.xebialabs.xlrelease.stress.parsers.dataset.User.Session
import com.xebialabs.xlrelease.stress.parsers.dataset._

import scala.concurrent.{ExecutionContext, Future}

class ReleasesHandler(val client: AkkaHttpXlrClient)(implicit val ec: ExecutionContext) {

  implicit def releasesHandler: Releases.Handler[Future] = new Releases.Handler[Future] {
    protected def importTemplate(session: User.Session, owner: User, template: Template): Future[Template.ID] =
      for {
        templateId <- client.importTemplate(template)(session)
        _ <- client.setTemplateTeams(templateId, List(templateOwner(owner, templateId), releaseAdmin(owner, templateId)))(session)
      } yield templateId

    protected def setTemplateTeam(session: Session, templateId: Template.ID, teams: Set[Team]): Future[Map[String, String]] =
      client.setTemplateTeams(templateId, teams.toList)(session)

    protected def createRelease(session: User.Session, templateId: Template.ID, createReleaseArgs: CreateReleaseArgs): Future[Template.ID] =
      client.createRelease(templateId, createReleaseArgs)(session)

  }
}

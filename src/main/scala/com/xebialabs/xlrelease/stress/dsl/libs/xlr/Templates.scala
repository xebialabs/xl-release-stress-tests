package com.xebialabs.xlrelease.stress.dsl.libs.xlr

import akka.http.scaladsl.server
import cats.implicits._
import com.github.nscala_time.time.Imports.DateTime
import com.xebialabs.xlrelease.stress.config.{XlrConfig, XlrServer}
import com.xebialabs.xlrelease.stress.domain._
import com.xebialabs.xlrelease.stress.dsl.DSL
import com.xebialabs.xlrelease.stress.dsl.http.libs
import com.xebialabs.xlrelease.stress.utils.DateFormat
import com.xebialabs.xlrelease.stress.utils.JsUtils
import freestyle.free._
import freestyle.free.implicits._
import spray.json._


class Templates[F[_]](server: XlrServer)(implicit protected val _api: DSL[F]) extends XlrLib[F] with DateFormat {

  def importXlr(template: Template)
               (implicit session: User.Session): Program[Template.ID] =
    for {
      _ <- log.debug(s"xlr.templates.importXlr(${template.name})")
      resp <- lib.http.zip.post(server.api(_ ?/ "templates" / "import"), template.xlrTemplate)
      content <- api.http.parseJson(resp)
      templateId <- lib.json.read(JsUtils.readFirstId)(content)
    } yield templateId

  def getTeams(templateId: Template.ID)
              (implicit session: User.Session): Program[Seq[Team]] =
    for {
      _ <- log.debug(s"xlr.templates.getTeams($templateId)")
      content <- lib.http.json.get(server.api(_ ?/ "templates" / "Applications" / templateId / "teams"))
      teams <- lib.json.read(JsUtils.readTeams)(content)
    } yield teams

  def setTeams(templateId: Template.ID, teams: Seq[Team])
              (implicit session: User.Session): Program[Map[String, String]] =
    for {
      _ <- log.debug(s"xlr.templates.setTeams($templateId, ${teams.map(_.teamName).mkString("[", ", ", "]")})")
      resp <- lib.http.json.post(server.api(_ ?/ "templates" / "Applications" / templateId / "teams"), teams.map(_.toJson).toJson)
      content <- api.http.parseJson(resp)
      teamIds <- lib.json.read(JsUtils.readTeamIds)(content)
    } yield teamIds

  def setScriptUser(templateId: Template.ID, scriptUser: Option[User])
                   (implicit session: User.Session): Program[Unit] = {
    val user = scriptUser.getOrElse(session.user)
    for {
      _ <- log.debug(s"xlr.templates.setScriptUser($templateId, $scriptUser)")
      resp <- lib.http.json.put(server.api(_ ?/ "templates" / "Applications" / templateId),
        JsObject(
          "id" -> JsNull,
          "scheduledStartDate" -> DateTime.now.toJson,
          "type" -> "xlrelease.Release".toJson,
          "scriptUsername" -> user.username.toJson,
          "scriptUserPassword" -> user.password.toJson
        )
      )
      _ <- api.http.discard(resp)
    } yield ()
  }
}
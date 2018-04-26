package com.xebialabs.xlrelease.stress.dsl.http.libs.http

import java.io.File

import akka.http.scaladsl.model.MediaTypes.`application/zip`
import akka.http.scaladsl.model._
import com.xebialabs.xlrelease.stress.config.defaults.http.client.{headers => defaultHeaders}
import com.xebialabs.xlrelease.stress.domain.HttpSession
import com.xebialabs.xlrelease.stress.dsl.http.DSL
import com.xebialabs.xlrelease.stress.dsl.http.libs.Api
import freestyle.free._
import freestyle.free.implicits._

trait PostZip[F[_]] extends Api[F] { self =>

  private def api: DSL[F] = self._api


  trait PostZipLib {
    def plain(uri: Uri, content: File, headers: List[HttpHeader] = defaultHeaders): Program[HttpResponse] = {
      val payload = Multipart.FormData(
        Multipart.FormData.BodyPart.fromFile(name = "file", `application/zip`, content)
      )
      api.http.post(uri, payload.toEntity, headers)
    }

    def apply(uri: Uri, content: File, headers: List[HttpHeader] = defaultHeaders)
             (implicit session: HttpSession): Program[HttpResponse] =
      plain(uri, content, headers ++ session.cookies.toList)
  }

  trait ZipLib {
    val post: PostZipLib = new PostZipLib {}
  }

  val zip: ZipLib = new ZipLib {}
}


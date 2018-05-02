package com.xebialabs.xlrelease.stress.dsl.http

import akka.http.scaladsl.model.{HttpHeader, HttpResponse, Uri}
import com.xebialabs.xlrelease.stress.dsl.http.libs.http.{Json, PostZip}
import com.xebialabs.xlrelease.stress.dsl.http.libs.json.ReadJson
import com.xebialabs.xlrelease.stress.dsl.http.libs.log.SessionLog
import com.xebialabs.xlrelease.stress.config.defaults.http.client.{headers => defaultHeaders}
import com.xebialabs.xlrelease.stress.domain.User
import freestyle.free._
import freestyle.free.implicits._

package object libs {

  class BaseLib[F[_]](protected val _api: DSL[F]) extends Api[F] { lib =>
    object http extends Json[F] with PostZip[F] {
      override val _api: DSL[F] = lib._api

      def delete(uri: Uri, headers: List[HttpHeader] = defaultHeaders)
                (implicit session: User.Session): Program[HttpResponse] = lib._api.http.delete(uri, headers ++ session.cookies.toList)
    }
    val json: ReadJson[F] = new ReadJson[F] {
      override val _api: DSL[F] = lib._api
    }
    object log {
      val session: SessionLog[F] = new SessionLog[F] {
        override val _api: DSL[F] = lib._api
      }
    }
  }

  trait Base[F[_]] extends Api[F] { self =>
    val lib: BaseLib[F] = new BaseLib[F](self._api)
    val log: SessionLog[F] = lib.log.session
  }

}

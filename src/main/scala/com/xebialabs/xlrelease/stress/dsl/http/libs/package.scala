package com.xebialabs.xlrelease.stress.dsl.http

import com.xebialabs.xlrelease.stress.dsl.http.libs.http.{Json, PostZip}
import com.xebialabs.xlrelease.stress.dsl.http.libs.json.ReadJson
import com.xebialabs.xlrelease.stress.dsl.http.libs.log.SessionLog

package object libs {

  class BaseLib[F[_]](protected val _api: DSL[F]) extends Api[F] { lib =>
    val http: Json[F] with PostZip[F] = new Json[F] with PostZip[F] {
      override val _api: DSL[F] = lib._api
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

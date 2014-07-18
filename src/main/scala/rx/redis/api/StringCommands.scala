package rx.redis.api

import rx.Observable

import rx.redis.resp.RespType


trait StringCommands {
  def get(key: String): Observable[RespType]

  def set[A : Writes](key: String, value: A): Observable[RespType]
}

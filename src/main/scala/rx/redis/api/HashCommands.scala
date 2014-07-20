package rx.redis.api

import rx.Observable

import rx.redis.resp.RespType


trait HashCommands {
  def hget(key: String, field: String): Observable[RespType]

  def hgetAll(key: String): Observable[RespType]
}

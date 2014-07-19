package rx.redis.api

import rx.Observable

import rx.redis.resp.RespType
import rx.redis.serialization.Writes


trait ConnectionCommands {
  def ping: Observable[RespType]

  def echo[A: Writes](msg: A): Observable[RespType]
}

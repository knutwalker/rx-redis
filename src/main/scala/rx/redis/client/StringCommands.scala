package rx.redis.client

import rx.Observable

import rx.redis.api
import rx.redis.commands.{Get, Set}
import rx.redis.resp.RespType
import rx.redis.serialization.Bytes


private[redis] trait StringCommands extends api.StringCommands { this: api.Client =>
  def get(key: String): Observable[RespType] =
    command(Get(key))

  def set[A: Bytes](key: String, value: A): Observable[RespType] =
    command(Set(key, value))

}

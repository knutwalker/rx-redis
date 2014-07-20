package rx.redis.client

import rx.Observable

import rx.redis.api
import rx.redis.commands.{HGetAll, HGet}
import rx.redis.resp.RespType


private[redis] trait HashCommands extends api.HashCommands { this: api.Client =>
  def hget(key: String, field: String): Observable[RespType] =
    command(HGet(key, field))

  def hgetAll(key: String): Observable[RespType] =
    command(HGetAll(key))
}

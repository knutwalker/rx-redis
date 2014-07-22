package rx.redis.api

import rx.Observable

import rx.redis.commands.{HGetAll, HGet}
import rx.redis.resp.RespType


trait HashCommands { this: Client =>
  def hget(key: String, field: String): Observable[RespType] =
    command(HGet(key, field))

  def hgetAll(key: String): Observable[RespType] =
    command(HGetAll(key))
}

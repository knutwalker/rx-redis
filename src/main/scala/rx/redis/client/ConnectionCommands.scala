package rx.redis.client

import rx.Observable

import rx.redis.api
import rx.redis.commands.{Echo, Ping}
import rx.redis.resp.RespType
import rx.redis.serialization.Bytes


private[redis] trait ConnectionCommands extends api.ConnectionCommands { this: RxRedisClient =>
  def ping: Observable[RespType] =
    command(Ping)

  def echo[A: Bytes](msg: A): Observable[RespType] =
    command(Echo(msg))
}

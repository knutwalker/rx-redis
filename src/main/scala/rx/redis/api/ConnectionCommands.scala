package rx.redis.api

import rx.Observable

import rx.redis.commands.{Echo, Ping}
import rx.redis.resp.RespType
import rx.redis.serialization.Bytes


trait ConnectionCommands { this: Client =>
  def ping(): Observable[RespType] =
    command(Ping)

  def echo[A](msg: A)(implicit A: Bytes[A]): Observable[RespType] =
    command(Echo(msg))
}

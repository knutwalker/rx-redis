package rx.redis.api

import rx.Observable

import rx.redis.resp.RespType
import rx.redis.serialization.Bytes


trait ConnectionCommands {
  def ping(): Observable[RespType]

  def echo[A](msg: A)(implicit A: Bytes[A]): Observable[RespType]
}

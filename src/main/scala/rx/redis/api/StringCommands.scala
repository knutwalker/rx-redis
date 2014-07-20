package rx.redis.api

import rx.Observable

import rx.redis.resp.RespType
import rx.redis.serialization.Bytes


trait StringCommands {
  def get(key: String): Observable[RespType]

  def set[A](key: String, value: A)(implicit A: Bytes[A]): Observable[RespType]
}

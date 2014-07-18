package rx.redis.api

import rx.lang.scala.Observable
import rx.redis.resp.RespType


trait StringCommands {
  def get(key: String): Observable[RespType]
  def set[A : Write](key: String, value: A): Observable[RespType]
}

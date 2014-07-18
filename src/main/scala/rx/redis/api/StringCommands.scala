package rx.redis.api

import rx.lang.scala.Observable
import rx.redis.resp.RespType


trait StringCommands {
  def get(key: String): Observable[RespType]
  def set[A](key: String, value: A)(implicit A: Write[A]): Observable[RespType]
}

package rx.redis.api

import rx.Observable

import rx.redis.resp.RespType

import scala.concurrent.duration.{Deadline, FiniteDuration}


trait KeyCommands {
  def del(key: String): Observable[RespType]

  def exists(key: String): Observable[RespType]

  def expire(key: String, expires: FiniteDuration): Observable[RespType]

  def expireAt(key: String, deadline: Deadline): Observable[RespType]

  def keys(pattern: String): Observable[RespType]

  def randomKey(): Observable[RespType]

  def ttl(key: String): Observable[RespType]
}

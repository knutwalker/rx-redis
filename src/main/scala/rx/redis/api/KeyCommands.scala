package rx.redis.api

import rx.Observable

import rx.redis.commands.{Ttl, RandomKey, Keys, ExpireAt, Expire, Exists, Del}
import rx.redis.resp.RespType

import scala.annotation.varargs
import scala.concurrent.duration.{Deadline, FiniteDuration}


trait KeyCommands { this: Client =>
  @varargs
  def del(keys: String*): Observable[RespType] =
    command(Del(keys: _*))

  def exists(key: String): Observable[RespType] =
    command(Exists(key))

  def expire(key: String, expires: FiniteDuration): Observable[RespType] =
    command(Expire(key, expires))

  def expireAt(key: String, deadline: Deadline): Observable[RespType] =
    command(ExpireAt(key, deadline))

  def keys(pattern: String): Observable[RespType] =
    command(Keys(pattern))

  def randomKey(): Observable[RespType] =
    command(RandomKey)

  def ttl(key: String): Observable[RespType] =
    command(Ttl(key))

}

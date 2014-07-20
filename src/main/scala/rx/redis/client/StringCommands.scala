package rx.redis.client

import rx.Observable

import rx.redis.api
import rx.redis.commands._
import rx.redis.resp.RespType
import rx.redis.serialization.Bytes

import scala.concurrent.duration.FiniteDuration


private[redis] trait StringCommands extends api.StringCommands { this: api.Client =>
  def get(key: String): Observable[RespType] =
    command(Get(key))

  def set[A: Bytes](key: String, value: A): Observable[RespType] =
    command(Set(key, value))

  def setEx[A: Bytes](key: String, value: A, expires: FiniteDuration): Observable[RespType] =
    command(SetEx(key, expires, value))

  def setNx[A: Bytes](key: String, value: A): Observable[RespType] =
    command(SetNx(key, value))

  def incr(key: String): Observable[RespType] =
    command(Incr(key))

  def incrBy(key: String, amount: Long): Observable[RespType] =
    command(IncrBy(key, amount))

  def decr(key: String): Observable[RespType] =
    command(Decr(key))

  def decrBy(key: String, amount: Long): Observable[RespType] =
    command(DecrBy(key, amount))

  def mget(keys: String*): Observable[RespType] =
    command(MGet(keys: _*))

  def mset[A: Bytes](items: (String, A)*): Observable[RespType] =
    command(MSet(items: _*))

  def strLen(key: String): Observable[RespType] =
    command(StrLen(key))
}

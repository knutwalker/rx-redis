package rx.redis.api

import rx.Observable

import rx.redis.commands._
import rx.redis.resp.RespType
import rx.redis.serialization.Bytes

import scala.annotation.varargs
import scala.concurrent.duration.FiniteDuration


trait StringCommands { this: Client =>
  def get(key: String): Observable[RespType] =
    command(Get(key))

  def set[A](key: String, value: A)(implicit A: Bytes[A]): Observable[RespType] =
    command(Set(key, value))

  def setEx[A](key: String, value: A, expires: FiniteDuration)(implicit A: Bytes[A]): Observable[RespType] =
    command(SetEx(key, expires, value))

  def setNx[A](key: String, value: A)(implicit A: Bytes[A]): Observable[RespType] =
    command(SetNx(key, value))

  def incr(key: String): Observable[RespType] =
    command(Incr(key))

  def incrBy(key: String, amount: Long): Observable[RespType] =
    command(IncrBy(key, amount))

  def decr(key: String): Observable[RespType] =
    command(Decr(key))

  def decrBy(key: String, amount: Long): Observable[RespType] =
    command(DecrBy(key, amount))

  @varargs
  def mget(keys: String*): Observable[RespType] =
    command(MGet(keys: _*))

  @varargs
  def mset[A](items: (String, A)*)(implicit A: Bytes[A]): Observable[RespType] =
    command(MSet(items: _*))

  def strLen(key: String): Observable[RespType] =
    command(StrLen(key))

}

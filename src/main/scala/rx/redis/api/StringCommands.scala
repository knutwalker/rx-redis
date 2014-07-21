package rx.redis.api

import rx.Observable

import rx.redis.resp.RespType
import rx.redis.serialization.Bytes

import scala.annotation.varargs
import scala.concurrent.duration.FiniteDuration


trait StringCommands {
  def get(key: String): Observable[RespType]

  def set[A](key: String, value: A)(implicit A: Bytes[A]): Observable[RespType]

  def setEx[A](key: String, value: A, expires: FiniteDuration)(implicit A: Bytes[A]): Observable[RespType]

  def setNx[A](key: String, value: A)(implicit A: Bytes[A]): Observable[RespType]

  def incr(key: String): Observable[RespType]

  def incrBy(key: String, amount: Long): Observable[RespType]

  def decr(key: String): Observable[RespType]

  def decrBy(key: String, amount: Long): Observable[RespType]

  @varargs
  def mget(keys: String*): Observable[RespType]

  @varargs
  def mset[A](items: (String, A)*)(implicit A: Bytes[A]): Observable[RespType]

  def strLen(key: String): Observable[RespType]
}

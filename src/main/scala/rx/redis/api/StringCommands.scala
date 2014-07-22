package rx.redis.api

import rx.Observable

import rx.redis.commands._
import rx.redis.serialization.{Bytes, Reads}

import scala.annotation.varargs
import scala.concurrent.duration.FiniteDuration


trait StringCommands { this: Client =>
  def get[A](key: String)(implicit A: Bytes[A]): Observable[Option[A]] =
    command(Get(key)).flatMap(Reads.bytes.obsOptT[A])

  def set[A](key: String, value: A)(implicit A: Bytes[A]): Observable[Boolean] =
    command(Set(key, value)).flatMap(Reads.bool.obs)

  def setEx[A](key: String, value: A, expires: FiniteDuration)(implicit A: Bytes[A]): Observable[Boolean] =
    command(SetEx(key, expires, value)).flatMap(Reads.bool.obs)

  def setNx[A](key: String, value: A)(implicit A: Bytes[A]): Observable[Boolean] =
    command(SetNx(key, value)).flatMap(Reads.bool.obs)

  def incr(key: String): Observable[Long] =
    command(Incr(key)).flatMap(Reads.int.obs)

  def incrBy(key: String, amount: Long): Observable[Long] =
    command(IncrBy(key, amount)).flatMap(Reads.int.obs)

  def decr(key: String): Observable[Long] =
    command(Decr(key)).flatMap(Reads.int.obs)

  def decrBy(key: String, amount: Long): Observable[Long] =
    command(DecrBy(key, amount)).flatMap(Reads.int.obs)

// cannot use varargs here due to SI-8743 (https://issues.scala-lang.org/browse/SI-8743)
//  @varargs
  def mget[A](keys: String*)(implicit A: Bytes[A]): Observable[Option[A]] = {
    command(MGet(keys: _*)).flatMap(Reads.list.obsMTOpt[A])
  }

  // temp due to SI-8743 (https://issues.scala-lang.org/browse/SI-8743)
  def mget[A](keys: Array[String])(implicit A: Bytes[A]): Observable[Option[A]] = {
    mget[A](keys: _*)
  }

  @varargs
  def mset[A](items: (String, A)*)(implicit A: Bytes[A]): Observable[Boolean] =
    command(MSet(items: _*)).flatMap(Reads.bool.obs)

  def strLen(key: String): Observable[Long] =
    command(StrLen(key)).flatMap(Reads.int.obs)

}

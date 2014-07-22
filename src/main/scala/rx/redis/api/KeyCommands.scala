package rx.redis.api

import rx.Observable

import rx.redis.commands.{Del, Exists, Expire, ExpireAt, Keys, RandomKey, Ttl}
import rx.redis.serialization.{Bytes, Reads}

import scala.concurrent.duration.{Deadline, FiniteDuration}


trait KeyCommands { this: Client =>
  // varargs is really really buggy... SI-1459 ?
  // @varargs
  def del(keys: String*): Observable[Long] =
    command(Del(keys: _*)).flatMap(Reads.int.obs)

  def del(keys: Array[String]): Observable[Long] =
    del(keys: _*)

  def exists(key: String): Observable[Boolean] =
    command(Exists(key)).flatMap(Reads.bool.obs)

  def expire(key: String, expires: FiniteDuration): Observable[Boolean] =
    command(Expire(key, expires)).flatMap(Reads.bool.obs)

  def expireAt(key: String, deadline: Deadline): Observable[Boolean] =
    command(ExpireAt(key, deadline)).flatMap(Reads.bool.obs)

  def keys[A: Bytes](pattern: String): Observable[A] =
    command(Keys(pattern)).flatMap(Reads.list.obsMT[A])

  def randomKey(): Observable[Option[String]] =
    command(RandomKey).flatMap(Reads.bytes.obsOptT[String])

  def ttl(key: String): Observable[Long] =
    command(Ttl(key)).flatMap(Reads.int.obs)

}

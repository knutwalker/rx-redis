/*
 * Copyright 2014 Paul Horn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.redis.api

import scala.concurrent.duration.{ Deadline, FiniteDuration }

import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable

import rx.redis.clients.RawClient
import rx.redis.resp.{ RespType, DataType }
import rx.redis.serialization.BytesFormat

final class Client(raw: RawClient) {
  def shutdown(): Observable[Unit] =
    raw.shutdown()

  def closedObservable: Observable[Unit] =
    raw.closedObservable

  def command(dt: DataType): Observable[RespType] =
    raw.command(dt)

  // ==============
  //  Key Commands
  // ==============

  def del(keys: String*): Observable[Long] =
    raw.del(keys: _*)

  def exists(key: String): Observable[Boolean] =
    raw.exists(key)

  def expire(key: String, expires: FiniteDuration): Observable[Boolean] =
    raw.expire(key, expires)

  def expireAt(key: String, deadline: Deadline): Observable[Boolean] =
    raw.expireAt(key, deadline)

  def keys(pattern: String): Observable[String] =
    raw.keys(pattern)

  def randomKey(): Observable[Option[String]] =
    raw.randomKey()

  def ttl(key: String): Observable[Long] =
    raw.ttl(key)

  // ================
  // String Commands
  // ================

  def getAs[A: BytesFormat](key: String): Observable[Option[A]] =
    raw.get[A](key)

  def get(key: String): Observable[Option[String]] =
    raw.get[String](key)

  def getBytes(key: String): Observable[Option[Array[Byte]]] =
    raw.get[Array[Byte]](key)

  def setAs[A: BytesFormat](key: String, value: A): Observable[Boolean] =
    raw.set[A](key: String, value)

  def set(key: String, value: String): Observable[Boolean] =
    raw.set[String](key, value)

  def set(key: String, value: Array[Byte]): Observable[Boolean] =
    raw.set[Array[Byte]](key, value)

  def setEx[A: BytesFormat](key: String, value: A, expires: FiniteDuration): Observable[Boolean] =
    raw.setEx[A](key, value, expires)

  def setNx[A: BytesFormat](key: String, value: A): Observable[Boolean] =
    raw.setNx[A](key, value)

  def incr(key: String): Observable[Long] =
    raw.incr(key)

  def incrBy(key: String, amount: Long): Observable[Long] =
    raw.incrBy(key, amount)

  def decr(key: String): Observable[Long] =
    raw.decr(key)

  def decrBy(key: String, amount: Long): Observable[Long] =
    raw.decrBy(key, amount)

  def mgetAs[A: BytesFormat](keys: String*): Observable[Option[A]] =
    raw.mget[A](keys: _*)

  def mget(keys: String*): Observable[Option[String]] =
    raw.mget[String](keys: _*)

  def mgetBytes(keys: String*): Observable[Option[Array[Byte]]] =
    raw.mget[Array[Byte]](keys: _*)

  def msetAs[A: BytesFormat](items: (String, A)*): Observable[Boolean] =
    raw.mset[A](items: _*)

  def mset(items: (String, String)*): Observable[Boolean] =
    raw.mset[String](items: _*)

  def msetBytes(items: (String, Array[Byte])*): Observable[Boolean] =
    raw.mset[Array[Byte]](items: _*)

  def strLen(key: String): Observable[Long] =
    raw.strLen(key)

  // ===============
  //  Hash Commands
  // ===============

  def hgetAs[A: BytesFormat](key: String, field: String): Observable[Option[A]] =
    raw.hget[A](key, field)

  def hget(key: String, field: String): Observable[Option[String]] =
    raw.hget[String](key, field)

  def hgetBytes(key: String, field: String): Observable[Option[Array[Byte]]] =
    raw.hget[Array[Byte]](key, field)

  def hgetAllAs[A: BytesFormat](key: String): Observable[(String, A)] =
    raw.hgetAll[A](key)

  def hgetAll(key: String): Observable[(String, String)] =
    raw.hgetAll[String](key)

  def hgetAllBytes(key: String): Observable[(String, Array[Byte])] =
    raw.hgetAll[Array[Byte]](key)

  // =====================
  //  Connection Commands
  // =====================

  def ping(): Observable[String] =
    raw.ping()

  def echo[A: BytesFormat](msg: A): Observable[A] =
    raw.echo[A](msg)
}

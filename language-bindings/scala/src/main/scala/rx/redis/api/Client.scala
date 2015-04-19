/*
 * Copyright 2014 – 2015 Paul Horn
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

import io.netty.buffer.ByteBuf

import scala.concurrent.duration.{ Deadline, FiniteDuration }

import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable

import rx.redis.clients.GenericClient
import rx.redis.resp.RespType
import rx.redis.serialization.{ ByteBufFormat, ByteBufReader, ByteBufWriter }

final class Client(underlying: GenericClient) {
  @deprecated("Use disconnect", "0.4.0")
  def shutdown(): Observable[Unit] =
    disconnect()

  private[api] def disconnect(): Observable[Unit] =
    underlying.disconnect()

  def command(dt: ByteBuf): Observable[RespType] =
    underlying.command(dt)

  // ==============
  //  Key Commands
  // ==============

  def del(keys: String*): Observable[Long] =
    underlying.del(keys: _*)

  def exists(key: String): Observable[Boolean] =
    underlying.exists(key)

  def expire(key: String, expires: FiniteDuration): Observable[Boolean] =
    underlying.expire(key, expires)

  def expireAt(key: String, deadline: Deadline): Observable[Boolean] =
    underlying.expireAt(key, deadline)

  def keys(pattern: String): Observable[String] =
    underlying.keys(pattern)

  def randomKey(): Observable[Option[String]] =
    underlying.randomKey()

  def ttl(key: String): Observable[Long] =
    underlying.ttl(key)

  // ================
  // String Commands
  // ================

  def getAs[A: ByteBufReader](key: String): Observable[Option[A]] =
    underlying.get[A](key)

  def get(key: String): Observable[Option[String]] =
    underlying.get[String](key)

  def getBytes(key: String): Observable[Option[Array[Byte]]] =
    underlying.get[Array[Byte]](key)

  def setAs[A: ByteBufWriter](key: String, value: A): Observable[Boolean] =
    underlying.set[A](key: String, value)

  def set(key: String, value: String): Observable[Boolean] =
    underlying.set[String](key, value)

  def set(key: String, value: Array[Byte]): Observable[Boolean] =
    underlying.set[Array[Byte]](key, value)

  def setEx[A: ByteBufWriter](key: String, value: A, expires: FiniteDuration): Observable[Boolean] =
    underlying.setEx[A](key, value, expires)

  def setNx[A: ByteBufWriter](key: String, value: A): Observable[Boolean] =
    underlying.setNx[A](key, value)

  def incr(key: String): Observable[Long] =
    underlying.incr(key)

  def incrBy(key: String, amount: Long): Observable[Long] =
    underlying.incrBy(key, amount)

  def decr(key: String): Observable[Long] =
    underlying.decr(key)

  def decrBy(key: String, amount: Long): Observable[Long] =
    underlying.decrBy(key, amount)

  def mgetAs[A: ByteBufReader](keys: String*): Observable[Option[A]] =
    underlying.mget[A](keys: _*)

  def mget(keys: String*): Observable[Option[String]] =
    underlying.mget[String](keys: _*)

  def mgetBytes(keys: String*): Observable[Option[Array[Byte]]] =
    underlying.mget[Array[Byte]](keys: _*)

  def msetAs[A: ByteBufWriter](items: (String, A)*): Observable[Boolean] =
    underlying.mset[A](items: _*)

  def mset(items: (String, String)*): Observable[Boolean] =
    underlying.mset[String](items: _*)

  def msetBytes(items: (String, Array[Byte])*): Observable[Boolean] =
    underlying.mset[Array[Byte]](items: _*)

  def strLen(key: String): Observable[Long] =
    underlying.strLen(key)

  // ===============
  //  Hash Commands
  // ===============

  def hgetAs[A: ByteBufReader](key: String, field: String): Observable[Option[A]] =
    underlying.hget[A](key, field)

  def hget(key: String, field: String): Observable[Option[String]] =
    underlying.hget[String](key, field)

  def hgetBytes(key: String, field: String): Observable[Option[Array[Byte]]] =
    underlying.hget[Array[Byte]](key, field)

  def hgetAllAs[A: ByteBufReader](key: String): Observable[(String, A)] =
    underlying.hgetAll[A](key)

  def hgetAll(key: String): Observable[(String, String)] =
    underlying.hgetAll[String](key)

  def hgetAllBytes(key: String): Observable[(String, Array[Byte])] =
    underlying.hgetAll[Array[Byte]](key)

  // =====================
  //  Connection Commands
  // =====================

  def ping(): Observable[String] =
    underlying.ping()

  def echo[A: ByteBufFormat](msg: A): Observable[A] =
    underlying.echo[A](msg)
}

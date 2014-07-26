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
package rx.redis.client

import io.reactivex.netty.RxNetty
import rx.Observable
import rx.redis.commands._
import rx.redis.protocol.Configurator
import rx.redis.resp.{ DataType, RespType }
import rx.redis.serialization.{ BytesFormat, Writes }

import scala.concurrent.duration.{ Deadline, FiniteDuration }

private[redis] object RawClient {
  def apply(host: String, port: Int, shareable: Boolean): RawClient = {
    val client = new DefaultClient(RxNetty.createTcpClient(host, port, new Configurator))
    if (shareable) new ThreadSafeClient(client) else client
  }
}

private[redis] abstract class RawClient {

  def command(cmd: DataType): Observable[RespType]
  def shutdown(): Observable[Unit]
  def closedObservable: Observable[Unit]

  def command[A](cmd: A)(implicit A: Writes[A]): Observable[RespType] =
    command(A.write(cmd))

  // ==============
  //  Key Commands
  // ==============

  def del(keys: String*): Observable[Long] =
    command(Del(keys: _*)).flatMap(Del.reads)

  def exists(key: String): Observable[Boolean] =
    command(Exists(key)).flatMap(Exists.reads)

  def expire(key: String, expires: FiniteDuration): Observable[Boolean] =
    command(Expire(key, expires)).flatMap(Expire.reads)

  def expireAt(key: String, deadline: Deadline): Observable[Boolean] =
    command(ExpireAt(key, deadline)).flatMap(ExpireAt.reads)

  def keys(pattern: String): Observable[String] =
    command(Keys(pattern)).flatMap(Keys.reads)

  def randomKey(): Observable[Option[String]] =
    command(RandomKey).flatMap(RandomKey.reads)

  def ttl(key: String): Observable[Long] =
    command(Ttl(key)).flatMap(Ttl.reads)

  // =================
  //  String Commands
  // =================

  def decr(key: String): Observable[Long] =
    command(Decr(key)).flatMap(Decr.reads)

  def decrBy(key: String, amount: Long): Observable[Long] =
    command(DecrBy(key, amount)).flatMap(Decr.reads)

  def get[A: BytesFormat](key: String): Observable[Option[A]] =
    command(Get(key)).flatMap(Get.reads)

  def incr(key: String): Observable[Long] =
    command(Incr(key)).flatMap(Incr.reads)

  def incrBy(key: String, amount: Long): Observable[Long] =
    command(IncrBy(key, amount)).flatMap(Incr.reads)

  def mget[A: BytesFormat](keys: String*): Observable[Option[A]] =
    command(MGet(keys: _*)).flatMap(MGet.reads)

  def mset[A: BytesFormat](items: (String, A)*): Observable[Boolean] =
    command(MSet(items: _*)).flatMap(MSet.reads)

  def set[A: BytesFormat](key: String, value: A): Observable[Boolean] =
    command(Set(key, value)).flatMap(Set.reads)

  def setEx[A: BytesFormat](key: String, value: A, expires: FiniteDuration): Observable[Boolean] =
    command(SetEx(key, expires, value)).flatMap(SetEx.reads)

  def setNx[A: BytesFormat](key: String, value: A): Observable[Boolean] =
    command(SetNx(key, value)).flatMap(SetNx.reads)

  def strLen(key: String): Observable[Long] =
    command(StrLen(key)).flatMap(StrLen.reads)

  // =====================
  //  Connection Commands
  // =====================

  def echo[A: BytesFormat](msg: A): Observable[A] =
    command(Echo(msg)).flatMap(Echo.reads)

  def ping(): Observable[String] =
    command(Ping).flatMap(Ping.reads)

  // ===============
  //  Hash Commands
  // ===============

  def hget[A: BytesFormat](key: String, field: String): Observable[Option[A]] =
    command(HGet(key, field)).flatMap(HGet.reads)

  def hgetAll[A: BytesFormat](key: String): Observable[(String, A)] =
    command(HGetAll(key)).flatMap(HGetAll.reads)
}

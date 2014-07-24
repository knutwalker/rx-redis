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
import rx.redis.resp.{DataType, RespType}
import rx.redis.serialization.{BytesFormat, Reads, Writes}

import scala.concurrent.duration.{Deadline, FiniteDuration}

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

  private def command[A](cmd: A)(implicit A: Writes[A]): Observable[RespType] =
    command(A.write(cmd))

  // ==============
  //  Key Commands
  // ==============

  def del(keys: String*): Observable[Long] =
    command(Del(keys: _*)).flatMap(Reads.int.obs)

  def exists(key: String): Observable[Boolean] =
    command(Exists(key)).flatMap(Reads.bool.obs)

  def expire(key: String, expires: FiniteDuration): Observable[Boolean] =
    command(Expire(key, expires)).flatMap(Reads.bool.obs)

  def expireAt(key: String, deadline: Deadline): Observable[Boolean] =
    command(ExpireAt(key, deadline)).flatMap(Reads.bool.obs)

  def keys[A: BytesFormat](pattern: String): Observable[A] =
    command(Keys(pattern)).flatMap(Reads.list.obsMT[A])

  def randomKey(): Observable[Option[String]] =
    command(RandomKey).flatMap(Reads.bytes.obsOptT[String])

  def ttl(key: String): Observable[Long] =
    command(Ttl(key)).flatMap(Reads.int.obs)

  // =================
  //  String Commands
  // =================

  def decr(key: String): Observable[Long] =
    command(Decr(key)).flatMap(Reads.int.obs)

  def decrBy(key: String, amount: Long): Observable[Long] =
    command(DecrBy(key, amount)).flatMap(Reads.int.obs)

  def get[A](key: String)(implicit A: BytesFormat[A]): Observable[Option[A]] =
    command(Get(key)).flatMap(Reads.bytes.obsOptT[A])

  def incr(key: String): Observable[Long] =
    command(Incr(key)).flatMap(Reads.int.obs)

  def incrBy(key: String, amount: Long): Observable[Long] =
    command(IncrBy(key, amount)).flatMap(Reads.int.obs)

  def mget[A](keys: String*)(implicit A: BytesFormat[A]): Observable[Option[A]] =
    command(MGet(keys: _*)).flatMap(Reads.list.obsMTOpt[A])

  def mset[A](items: (String, A)*)(implicit A: BytesFormat[A]): Observable[Boolean] =
    command(MSet(items: _*)).flatMap(Reads.bool.obs)

  def set[A](key: String, value: A)(implicit A: BytesFormat[A]): Observable[Boolean] =
    command(Set(key, value)).flatMap(Reads.bool.obs)

  def setEx[A](key: String, value: A, expires: FiniteDuration)(implicit A: BytesFormat[A]): Observable[Boolean] =
    command(SetEx(key, expires, value)).flatMap(Reads.bool.obs)

  def setNx[A](key: String, value: A)(implicit A: BytesFormat[A]): Observable[Boolean] =
    command(SetNx(key, value)).flatMap(Reads.bool.obs)

  def strLen(key: String): Observable[Long] =
    command(StrLen(key)).flatMap(Reads.int.obs)


  // =====================
  //  Connection Commands
  // =====================

  def echo[A](msg: A)(implicit A: BytesFormat[A]): Observable[A] =
    command(Echo(msg)).flatMap(Reads.bytes.obsT[A])

  def ping(): Observable[String] =
    command(Ping).flatMap(Reads.bytes.obsT[String])

  // ===============
  //  Hash Commands
  // ===============

  def hget[A: BytesFormat](key: String, field: String): Observable[Option[A]] =
    command(HGet(key, field)).flatMap(Reads.bytes.obsOptT[A])

  def hgetAll[A: BytesFormat](key: String): Observable[(String, A)] =
    command(HGetAll(key)).flatMap(Reads.unzip.obsAB[String, A])
}

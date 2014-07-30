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

package rx.redis.clients

import rx.Observable
import rx.exceptions.OnErrorThrowable
import rx.functions.Func1
import rx.redis.commands._
import rx.redis.pipeline.RxOnNettyClient
import rx.redis.resp.{ DataType, RespType }
import rx.redis.serialization.{ BytesFormat, ReadsM, Reads, Writes }

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.concurrent.duration.{ Deadline, FiniteDuration }

object RawClient {
  def apply(host: String, port: Int): RawClient = {
    val netty = new RxOnNettyClient[DataType, RespType](host, port)
    new DefaultClient(netty)
  }
}

abstract class RawClient {

  def command(cmd: DataType): Observable[RespType]
  def shutdown(): Observable[Unit]
  def closedObservable: Observable[Unit]

  def command[A](cmd: A)(implicit A: Writes[A]): Observable[RespType] =
    command(A.write(cmd))

  private def withError[A](o: Observable[A]): Observable[A] = {
    o.onErrorFlatMap(new Func1[OnErrorThrowable, Observable[A]] {
      def call(t1: OnErrorThrowable): Observable[A] = {
        Observable.error(
          new IllegalArgumentException(s"Cannot interpret ${t1.getValue}", t1.getCause))
      }
    })
  }

  private def single[A](cmd: A)(implicit A: Writes[A], R: Reads[A]): Observable[R.R] = withError {
    command(A.write(cmd)).map[R.R](new Func1[RespType, R.R] {
      def call(t1: RespType): R.R = R.read(t1)
    })
  }

  private def multiple[A](cmd: A)(implicit A: Writes[A], R: ReadsM[A, List]): Observable[R.R] = withError {
    command(A.write(cmd)).flatMap[R.R](new Func1[RespType, Observable[R.R]] {
      def call(t1: RespType): Observable[R.R] = Observable.from(R.read(t1).asJava)
    })
  }

  // ==============
  //  Key Commands
  // ==============

  def del(keys: String*): Observable[Long] =
    single(Del(keys: _*))

  def exists(key: String): Observable[Boolean] =
    single(Exists(key))

  def expire(key: String, expires: FiniteDuration): Observable[Boolean] =
    single(Expire(key, expires))

  def expireAt(key: String, deadline: Deadline): Observable[Boolean] =
    single(ExpireAt(key, deadline))

  def keys(pattern: String): Observable[String] =
    multiple(Keys(pattern))

  def randomKey(): Observable[Option[String]] =
    single(RandomKey)

  def ttl(key: String): Observable[Long] =
    single(Ttl(key))

  // =================
  //  String Commands
  // =================

  def decr(key: String): Observable[Long] =
    single(Decr(key))

  def decrBy(key: String, amount: Long): Observable[Long] =
    single(DecrBy(key, amount))

  def get[A: BytesFormat](key: String): Observable[Option[A]] =
    single(Get(key))

  def incr(key: String): Observable[Long] =
    single(Incr(key))

  def incrBy(key: String, amount: Long): Observable[Long] =
    single(IncrBy(key, amount))

  def mget[A: BytesFormat](keys: String*): Observable[Option[A]] =
    multiple(MGet(keys: _*))

  def mset[A: BytesFormat](items: (String, A)*): Observable[Boolean] =
    single(MSet(items: _*))

  def set[A: BytesFormat](key: String, value: A): Observable[Boolean] =
    single(Set(key, value))

  def setEx[A: BytesFormat](key: String, value: A, expires: FiniteDuration): Observable[Boolean] =
    single(SetEx(key, expires, value))

  def setNx[A: BytesFormat](key: String, value: A): Observable[Boolean] =
    single(SetNx(key, value))

  def strLen(key: String): Observable[Long] =
    single(StrLen(key))

  // =====================
  //  Connection Commands
  // =====================

  def echo[A: BytesFormat](msg: A): Observable[A] =
    single(Echo(msg))

  def ping(): Observable[String] =
    single(Ping)

  // ===============
  //  Hash Commands
  // ===============

  def hget[A: BytesFormat](key: String, field: String): Observable[Option[A]] =
    single(HGet(key, field))

  def hgetAll[A: BytesFormat](key: String): Observable[(String, A)] =
    multiple(HGetAll(key))
}

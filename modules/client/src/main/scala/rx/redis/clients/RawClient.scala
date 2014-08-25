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

import rx.Observable.OnSubscribe
import rx.subjects.AsyncSubject
import io.netty.channel.{ ChannelFutureListener, ChannelFuture }

import rx.redis.pipeline.{ RxNettyClient, NettyClient }

import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.concurrent.duration.{ Deadline, FiniteDuration }

import rx.{ Subscriber, Observable }
import rx.functions.Func1

import rx.redis.commands._
import rx.redis.resp.RespType
import rx.redis.serialization.{ BytesFormat, Id, Reads, Writes }

import scala.util.control.NoStackTrace

object RawClient {
  def apply(host: String, port: Int): RawClient = {
    new DefaultClient(RxNettyClient(host, port))
  }
}

abstract class RawClient {
  protected val netty: NettyClient

  protected def eagerObservable(f: ChannelFuture): Observable[Unit] = {
    val subject = AsyncSubject.create[Unit]()
    f.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture): Unit = {
        if (future.isSuccess) {
          subject.onNext(())
          subject.onCompleted()
        } else {
          subject.onError(future.cause())
        }
      }
    })
    subject
  }

  protected def eagerObservable(f: Observable[Unit]): Observable[Unit] = {
    f.subscribe()
    f
  }

  protected def lazyObservable(f: ⇒ ChannelFuture): Observable[Unit] = {
    Observable.create[Unit](new OnSubscribe[Unit] {
      def call(subject: Subscriber[_ >: Unit]): Unit = {
        f.addListener(new ChannelFutureListener {
          def operationComplete(future: ChannelFuture): Unit = {
            if (!subject.isUnsubscribed) {
              if (future.isSuccess) {
                subject.onNext(())
                subject.onCompleted()
              } else {
                subject.onError(future.cause())
              }
            }
          }
        })
      }
    })
  }

  // =========
  //  Closing
  // =========

  private val isClosed = new AtomicBoolean(false)
  private val alreadyClosed: Observable[Unit] =
    Observable.error(new IllegalStateException("Client has already shutdown.") with NoStackTrace)

  protected def closeClient(): Observable[Unit]

  def disconnect(): Observable[Unit] = {
    if (isClosed.compareAndSet(false, true)) {
      closeClient()
    } else {
      alreadyClosed
    }
  }

  // ==================
  //  Generic Commands
  // ==================

  def command(cmd: RespType): Observable[RespType]

  def command[A](cmd: A)(implicit A: Writes[A]): Observable[RespType] =
    command(A.write(cmd))

  private def withError[A](o: Observable[A]): Observable[A] = {
    o.onErrorResumeNext(new Func1[Throwable, Observable[A]] {
      def call(t1: Throwable): Observable[A] = {
        Observable.error(
          new IllegalArgumentException(s"Cannot interpret value", t1))
      }
    })
  }

  private def single[A](cmd: A)(implicit A: Writes[A], R: Reads[A, Id]): Observable[R.R] = withError {
    command(A.write(cmd)).map[R.R](new Func1[RespType, R.R] {
      def call(t1: RespType): R.R = R.read(t1)
    })
  }

  private def multiple[A](cmd: A)(implicit A: Writes[A], R: Reads[A, List]): Observable[R.R] = withError {
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

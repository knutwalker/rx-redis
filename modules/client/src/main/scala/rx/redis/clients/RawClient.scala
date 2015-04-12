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

package rx.redis.clients

import rx.redis.RedisCommand
import rx.redis.commands._
import rx.redis.pipeline.{ NettyClient, RxNettyClient }
import rx.redis.resp.RespType
import rx.redis.serialization.{ writeStringAsRedisCommand, ByteBufFormat, ByteBufReader, ByteBufWriter, Id, Reads, Writes }

import rx.Observable.OnSubscribe
import rx.functions.Func1
import rx.subjects.AsyncSubject
import rx.{ Observable, Subscriber }

import io.netty.buffer.ByteBuf
import io.netty.channel.{ ChannelFuture, ChannelFutureListener }

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.concurrent.duration.{ Deadline, FiniteDuration }
import util.control.{ NoStackTrace, NonFatal }

import java.util.concurrent.atomic.AtomicBoolean

object RawClient {
  def apply(host: String, port: Int): RawClient = {
    new DefaultClient(RxNettyClient(host, port))
  }
}

abstract class RawClient {
  protected val netty: NettyClient

  protected final def eagerObservable(f: ChannelFuture): Observable[Unit] = {
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

  protected final def eagerObservable(f: Observable[Unit]): Observable[Unit] = {
    f.subscribe()
    f
  }

  protected final def lazyObservable(f: ⇒ ChannelFuture): Observable[Unit] = {
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

  private[this] val isClosed = new AtomicBoolean(false)
  private[this] val alreadyClosed: Observable[Unit] =
    Observable.error(new IllegalStateException("Client has already shutdown.") with NoStackTrace)

  protected def closeClient(): Observable[Unit]

  final def disconnect(): Observable[Unit] = {
    if (isClosed.compareAndSet(false, true)) {
      closeClient()
    } else {
      alreadyClosed
    }
  }

  // ==================
  //  Generic Commands
  // ==================

  def command(bb: ByteBuf): Observable[RespType]

  final def command(s: String with RedisCommand): Observable[RespType] =
    command(writeStringAsRedisCommand(s))

  final def command[A](cmd: A)(implicit A: Writes[A]): Observable[RespType] = {
    val buf = netty.alloc.buffer()
    try {
      A.write(buf, cmd)
    } catch {
      case NonFatal(ex) ⇒
        buf.release()
        Observable.error(ex)
    }
    if (buf.isReadable) {
      command(buf)
    } else {
      buf.release()
      Observable.empty()
    }
  }

  private[this] def withError[A](o: Observable[A]): Observable[A] = {
    o.onErrorResumeNext(new Func1[Throwable, Observable[A]] {
      def call(t1: Throwable): Observable[A] = {
        Observable.error(
          new IllegalArgumentException(s"Cannot interpret value", t1))
      }
    })
  }

  private[this] def single[A](cmd: A)(implicit A: Writes[A], R: Reads[A, Id]): Observable[R.R] = withError {
    command(cmd)(A).map[R.R](new Func1[RespType, R.R] {
      def call(t1: RespType): R.R = R.read(t1)
    })
  }

  private[this] def multiple[A](cmd: A)(implicit A: Writes[A], R: Reads[A, List]): Observable[R.R] = withError {
    command(cmd)(A).flatMap[R.R](new Func1[RespType, Observable[R.R]] {
      def call(t1: RespType): Observable[R.R] = Observable.from(R.read(t1).asJava)
    })
  }

  // ==============
  //  Key Commands
  // ==============

  final def del(keys: String*): Observable[Long] =
    single(Del(keys: _*))

  final def exists(key: String): Observable[Boolean] =
    single(Exists(key))

  final def expire(key: String, expires: FiniteDuration): Observable[Boolean] =
    single(Expire(key, expires))

  final def expireAt(key: String, deadline: Deadline): Observable[Boolean] =
    single(ExpireAt(key, deadline))

  final def keys(pattern: String): Observable[String] =
    multiple(Keys(pattern))

  final def randomKey(): Observable[Option[String]] =
    single(RandomKey)

  final def ttl(key: String): Observable[Long] =
    single(Ttl(key))

  // =================
  //  String Commands
  // =================

  final def decr(key: String): Observable[Long] =
    single(Decr(key))

  final def decrBy(key: String, amount: Long): Observable[Long] =
    single(DecrBy(key, amount))

  final def get[A: ByteBufReader](key: String): Observable[Option[A]] =
    single(Get(key))

  final def incr(key: String): Observable[Long] =
    single(Incr(key))

  final def incrBy(key: String, amount: Long): Observable[Long] =
    single(IncrBy(key, amount))

  final def mget[A: ByteBufReader](keys: String*): Observable[Option[A]] =
    multiple(MGet(keys: _*))

  final def mset[A: ByteBufWriter](items: (String, A)*): Observable[Boolean] =
    single(MSet(items: _*))

  final def set[A: ByteBufWriter](key: String, value: A): Observable[Boolean] =
    single(Set(key, value))

  final def setEx[A: ByteBufWriter](key: String, value: A, expires: FiniteDuration): Observable[Boolean] =
    single(SetEx(key, expires, value))

  final def setNx[A: ByteBufWriter](key: String, value: A): Observable[Boolean] =
    single(SetNx(key, value))

  final def strLen(key: String): Observable[Long] =
    single(StrLen(key))

  // =====================
  //  Connection Commands
  // =====================

  final def echo[A: ByteBufFormat](msg: A): Observable[A] =
    single(Echo(msg))

  final def ping(): Observable[String] =
    single(Ping)

  // ===============
  //  Hash Commands
  // ===============

  final def hget[A: ByteBufReader](key: String, field: String): Observable[Option[A]] =
    single(HGet(key, field))

  final def hgetAll[A: ByteBufReader](key: String): Observable[(String, A)] =
    multiple(HGetAll(key))
}

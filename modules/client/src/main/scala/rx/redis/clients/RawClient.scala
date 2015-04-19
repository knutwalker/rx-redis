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
import rx.redis.pipeline.{ OperatorDecode, NettyClient, RxNettyClient }
import rx.redis.resp.RespType
import rx.redis.serialization.{ writeStringAsRedisCommand, ByteBufFormat, ByteBufReader, ByteBufWriter, Reads, Writes }

import rx.Observable.OnSubscribe
import rx.subjects.AsyncSubject
import rx.{ Observable, Subscriber }

import io.netty.buffer.ByteBuf
import io.netty.channel.{ ChannelFuture, ChannelFutureListener }

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

  private[this] def run[A](cmd: A)(implicit A: Writes[A], R: Reads[A]): Observable[R.R] =
    command(cmd)(A).lift(new OperatorDecode[A, R.R](R))

  // ==============
  //  Key Commands
  // ==============

  final def del(keys: String*): Observable[Long] =
    run(Del(keys: _*))

  final def exists(key: String): Observable[Boolean] =
    run(Exists(key))

  final def expire(key: String, expires: FiniteDuration): Observable[Boolean] =
    run(Expire(key, expires))

  final def expireAt(key: String, deadline: Deadline): Observable[Boolean] =
    run(ExpireAt(key, deadline))

  final def keys(pattern: String): Observable[String] =
    run(Keys(pattern))

  final def randomKey(): Observable[Option[String]] =
    run(RandomKey)

  final def ttl(key: String): Observable[Long] =
    run(Ttl(key))

  // =================
  //  String Commands
  // =================

  final def decr(key: String): Observable[Long] =
    run(Decr(key))

  final def decrBy(key: String, amount: Long): Observable[Long] =
    run(DecrBy(key, amount))

  final def get[A: ByteBufReader](key: String): Observable[Option[A]] =
    run(Get(key))

  final def incr(key: String): Observable[Long] =
    run(Incr(key))

  final def incrBy(key: String, amount: Long): Observable[Long] =
    run(IncrBy(key, amount))

  final def mget[A: ByteBufReader](keys: String*): Observable[Option[A]] =
    run(MGet(keys: _*))

  final def mset[A: ByteBufWriter](items: (String, A)*): Observable[Boolean] =
    run(MSet(items: _*))

  final def set[A: ByteBufWriter](key: String, value: A): Observable[Boolean] =
    run(Set(key, value))

  final def setEx[A: ByteBufWriter](key: String, value: A, expires: FiniteDuration): Observable[Boolean] =
    run(SetEx(key, expires, value))

  final def setNx[A: ByteBufWriter](key: String, value: A): Observable[Boolean] =
    run(SetNx(key, value))

  final def strLen(key: String): Observable[Long] =
    run(StrLen(key))

  // =====================
  //  Connection Commands
  // =====================

  final def echo[A: ByteBufFormat](msg: A): Observable[A] =
    run(Echo(msg))

  final def ping(): Observable[String] =
    run(Ping)

  // ===============
  //  Hash Commands
  // ===============

  final def hget[A: ByteBufReader](key: String, field: String): Observable[Option[A]] =
    run(HGet(key, field))

  final def hgetAll[A: ByteBufReader](key: String): Observable[(String, A)] =
    run(HGetAll(key))
}

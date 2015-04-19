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

import rx.redis.pipeline.{ NettyClient, RxNettyClient }

import rx.Observable.OnSubscribe
import rx.subjects.AsyncSubject
import rx.{ Observable, Subscriber }

import io.netty.buffer.ByteBufAllocator
import io.netty.channel.{ ChannelFuture, ChannelFutureListener }

import util.control.NoStackTrace

import java.util.concurrent.atomic.AtomicBoolean

object RawClient {
  def apply(host: String, port: Int): RawClient = {
    new DefaultClient(RxNettyClient(host, port))
  }
}

abstract class RawClient extends GenericClient {

  final def disconnect(): Observable[Unit] = {
    if (isClosed.compareAndSet(false, true)) {
      closeClient()
    } else {
      alreadyClosed
    }
  }

  protected val netty: NettyClient

  protected def closeClient(): Observable[Unit]

  protected final def alloc: ByteBufAllocator = netty.alloc

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

  private[this] val isClosed = new AtomicBoolean(false)
  private[this] val alreadyClosed: Observable[Unit] =
    Observable.error(new IllegalStateException("Client has already shutdown.") with NoStackTrace)
}

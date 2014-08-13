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

package rx.redis.pipeline

import rx.Observable.OnSubscribe
import rx.redis.channel.SharedNioEventLoopGroup
import rx.{ Observable, Subscriber, Observer }
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ EventLoopGroup, Channel, ChannelFuture, ChannelFutureListener, ChannelOption }
import io.netty.util.concurrent.{ GenericFutureListener, Future, DefaultThreadFactory }

import rx.redis.resp.{ DataType, RespType }

import scala.language.implicitConversions

object RxNettyClient {
  private final val threadFactory = new DefaultThreadFactory("rx-redis", true)
  private final val eventLoopGroup = new SharedNioEventLoopGroup(0, threadFactory)

  def apply(host: String, port: Int): NettyClient = {
    val channelInitializer = new RxChannelInitializer(optimizeForThroughput = true)
    val bootstrap = {
      val b = new Bootstrap()
      b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT).
        option(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE).
        option(ChannelOption.SO_SNDBUF, Int.box(1024 * 1024)).
        option(ChannelOption.SO_RCVBUF, Int.box(1024 * 1024)).
        option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, Int.box(10 * 64 * 1024)).
        channel(classOf[NioSocketChannel]).
        group(eventLoopGroup.retain()).
        handler(channelInitializer)
    }

    val channel = bootstrap.connect(host, port).sync().channel()
    new RxNettyClient(channel)
  }

  private final class ChannelCloseSubscribe(channel: Channel) extends OnSubscribe[Unit] {
    def call(subscriber: Subscriber[_ >: Unit]): Unit =
      channel.close().addListener(
        new ChannelCloseListener(subscriber, channel.eventLoop.parent))
  }

  private object NettyFutureSubscription {
    def apply[F <: Future[_], S <: Subscriber[_ >: Unit]](future: F, subscriber: S, onNext: ⇒ Unit): Unit =
      if (subscriber.isUnsubscribed) {
        future.cancel(true)
      } else if (future.isCancelled) {
        subscriber.unsubscribe()
      } else if (future.isSuccess) {
        subscriber.onNext(())
        onNext
      } else {
        subscriber.onError(future.cause())
      }
  }

  private final class ChannelCloseListener[S <: Subscriber[_ >: Unit]](subscriber: S, eventLoopGroup: EventLoopGroup) extends ChannelFutureListener {
    def operationComplete(future: ChannelFuture): Unit =
      NettyFutureSubscription(
        future, subscriber,
        eventLoopGroup.shutdownGracefully().addListener(new ShutdownListener(subscriber)))
  }

  private final class ShutdownListener[F <: Future[_], S <: Subscriber[_ >: Unit]](subscriber: S) extends GenericFutureListener[F] {
    def operationComplete(future: F): Unit =
      NettyFutureSubscription(
        future, subscriber,
        subscriber.onCompleted())
  }
}

private[redis] class RxNettyClient(channel: Channel) extends NettyClient {
  @inline
  private final implicit def writeToRunnable(f: ⇒ ChannelFuture): Runnable = new Runnable {
    def run(): Unit = f
  }

  private final val eventLoop = channel.eventLoop()
  private final val pipeline = channel.pipeline()
  private final val emptyPromise = channel.voidPromise()

  def send(data: DataType, receiver: Observer[RespType]): Unit = {
    eventLoop.execute(pipeline.write(AdapterAction.writeAndFlush(data, receiver), emptyPromise))
  }

  def buffer(data: DataType, receiver: Observer[RespType]): Unit = {
    eventLoop.execute(pipeline.write(AdapterAction.write(data, receiver), emptyPromise))
  }

  def flush(): ChannelFuture = {
    val promise = channel.newPromise()
    eventLoop.execute(pipeline.write(AdapterAction.flush(), emptyPromise))
    promise
  }

  def close(): Observable[Unit] = {
    Observable.create(new RxNettyClient.ChannelCloseSubscribe(channel)).cache()
  }
}

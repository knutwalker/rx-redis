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

package rx.redis.pipeline

import rx.Observable.OnSubscribe
import rx.redis.channel.SharedNioEventLoopGroup
import rx.{ Observable, Subscriber, Observer }
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.{ ByteBuf, ByteBufAllocator, PooledByteBufAllocator }
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ EventLoopGroup, Channel, ChannelFuture, ChannelFutureListener, ChannelOption }
import io.netty.util.concurrent.{ GenericFutureListener, Future, DefaultThreadFactory }

import rx.redis.resp.RespType

import scala.language.implicitConversions

object RxNettyClient {
  private[this] final val threadFactory = new DefaultThreadFactory("rx-redis", true)
  private[this] final val eventLoopGroup = new SharedNioEventLoopGroup(0, threadFactory)

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

  private def channelClose(channel: Channel): OnSubscribe[Unit] =
    new ChannelCloseSubscribe(channel)

  private[this] final class ChannelCloseSubscribe(channel: Channel) extends OnSubscribe[Unit] {
    def call(subscriber: Subscriber[_ >: Unit]): Unit =
      channel.close().addListener(
        new ChannelCloseListener(subscriber, channel.eventLoop.parent))
  }

  private[this] final class ChannelCloseListener[S <: Subscriber[_ >: Unit]](subscriber: S, eventLoopGroup: EventLoopGroup) extends ChannelFutureListener {
    def operationComplete(future: ChannelFuture): Unit =
      futureSubscription(
        future, subscriber,
        eventLoopGroup.shutdownGracefully().addListener(new ShutdownListener(subscriber)))
  }

  private[this] final class ShutdownListener[F <: Future[_], S <: Subscriber[_ >: Unit]](subscriber: S) extends GenericFutureListener[F] {
    def operationComplete(future: F): Unit =
      futureSubscription(
        future, subscriber,
        subscriber.onCompleted())
  }

  private[this] final def futureSubscription[F <: Future[_], S <: Subscriber[_ >: Unit]](future: F, subscriber: S, onNext: ⇒ Unit): Unit =
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

private[redis] class RxNettyClient(channel: Channel) extends NettyClient {
  @inline
  private[this] final implicit def writeToRunnable(f: ⇒ ChannelFuture): Runnable = new Runnable {
    def run(): Unit = f
  }

  val alloc: ByteBufAllocator = channel.alloc()

  private[this] final val eventLoop = channel.eventLoop()
  private[this] final val pipeline = channel.pipeline()

  private[this] final val emptyPromise = channel.voidPromise()
  private[this] final val flushTask = new Runnable {
    def run(): Unit = pipeline.flush()
  }

  def send(bb: ByteBuf, receiver: Observer[RespType]): Unit = {
    eventLoop.execute(pipeline.writeAndFlush(AdapterAction(bb, receiver), emptyPromise))
  }

  def buffer(data: RespType, receiver: Observer[RespType]): Unit = {
    //    eventLoop.execute(pipeline.write(AdapterAction(data, receiver), emptyPromise))
  }

  def flush(): ChannelFuture = {
    val promise = channel.newPromise()
    eventLoop.execute(flushTask)
    promise
  }

  def close(): Observable[Unit] = {
    Observable.create(RxNettyClient.channelClose(channel)).cache()
  }
}

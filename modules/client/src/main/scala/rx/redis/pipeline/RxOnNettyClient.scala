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

import rx.functions.{ Func1, Func2 }
import rx.schedulers.Schedulers
import rx.subjects.{ AsyncSubject, PublishSubject }
import rx.{ Observable, Observer }
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ ChannelFuture, ChannelFutureListener, ChannelHandlerContext, ChannelOption }
import io.netty.util.concurrent.DefaultThreadFactory

object RxOnNettyClient {
  private final class WriteOnChannel[Send <: AnyRef, Recv <: AnyRef](ctx: ChannelHandlerContext)
      extends Func1[(Send, Observer[Recv]), Observer[Recv]] {

    def call(t1: (Send, Observer[Recv])): Observer[Recv] = {
      ctx.writeAndFlush(t1._1)
      t1._2
    }
  }
  private final class ReturnToSender[Recv <: AnyRef] extends Func2[Observer[_ >: Recv], Recv, Unit] {
    def call(t1: Observer[_ >: Recv], t2: Recv): Unit = {
      t1.onNext(t2)
      t1.onCompleted()
    }
  }
  private object DiscardingObserver {
    def apply(o: Observable[Unit]): Observable[Unit] = {
      val s = AsyncSubject.create[Unit]()
      o.subscribe(new DiscardingObserver(s))
      s
    }
  }

  private final class DiscardingObserver(target: Observer[_]) extends Observer[Unit] {
    def onNext(t: Unit): Unit = ()
    def onError(error: Throwable): Unit = target.onError(error)
    def onCompleted(): Unit = target.onCompleted()
  }
}

private[redis] final class RxOnNettyClient[Send <: AnyRef, Recv <: AnyRef](host: String, port: Int) extends NettyClient[Send, Recv] {
  import rx.redis.pipeline.RxOnNettyClient.{ DiscardingObserver, ReturnToSender, WriteOnChannel }

  private val inputSubject =
    PublishSubject.create[(Send, Observer[Recv])]()

  private val responseSubject =
    PublishSubject.create[Recv]()

  private val channelInitializer =
    new RxChannelInitializer(responseSubject)

  private val threadFactory =
    new DefaultThreadFactory("rx-redis", true)

  private val eventLoopGroup =
    new NioEventLoopGroup(1, threadFactory)

  private val scheduler =
    Schedulers.from(eventLoopGroup)

  private val bootstrap = {
    val b = new Bootstrap()
    b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
    b.option(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE)
    b.option(ChannelOption.TCP_NODELAY, java.lang.Boolean.TRUE)
    b.channel(classOf[NioSocketChannel]).
      group(eventLoopGroup).
      handler(channelInitializer)
  }
  private val channel = bootstrap.connect(host, port).sync().channel()
  private val channelContext = channel.pipeline().lastContext

  private val senders: Observable[Observer[Recv]] =
    inputSubject.
      onBackpressureBuffer().
      observeOn(scheduler).
      map(new WriteOnChannel[Send, Recv](channelContext))

  private val returnedResponses =
    senders.zipWith[Recv, Unit](responseSubject, new ReturnToSender[Recv])

  def send(data: Send, receiver: Observer[Recv]): Unit = {
    inputSubject.onNext(data -> receiver)
  }

  val closed: Observable[Unit] = {
    DiscardingObserver(returnedResponses)
  }

  def close(): AsyncSubject[Unit] = {
    val closedSubject = AsyncSubject.create[Unit]()
    channelContext.close.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture): Unit = {
        try {
          if (future.isSuccess) {
            closedSubject.onCompleted()
          } else {
            closedSubject.onError(future.cause)
          }
        } finally {
          bootstrap.group.shutdownGracefully
        }
      }
    })
    inputSubject.onCompleted()
    closedSubject
  }
}

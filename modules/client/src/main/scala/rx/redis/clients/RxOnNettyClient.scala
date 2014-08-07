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

import rx.subjects.AsyncSubject
import rx.{ Observable, Observer }
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ ChannelFuture, ChannelFutureListener, ChannelOption }
import io.netty.util.concurrent.DefaultThreadFactory

import rx.redis.pipeline.{ RxAdapter, RxChannelInitializer }
import rx.redis.resp.{ DataType, RespType }

private[redis] final class RxOnNettyClient(host: String, port: Int) extends NettyClient {

  private val closedSubject =
    AsyncSubject.create[Unit]()

  private val channelInitializer =
    new RxChannelInitializer(optimizeForThroughput = true)

  private val threadFactory =
    new DefaultThreadFactory("rx-redis", true)

  private val eventLoopGroup =
    new NioEventLoopGroup(1, threadFactory)

  private val bootstrap = {
    val b = new Bootstrap()
    b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT).
      option(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE).
      option(ChannelOption.SO_SNDBUF, Int.box(1024 * 1024)).
      option(ChannelOption.SO_RCVBUF, Int.box(1024 * 1024)).
      option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, Int.box(10 * 64 * 1024)).
      channel(classOf[NioSocketChannel]).
      group(eventLoopGroup).
      handler(channelInitializer)
  }
  private val channel = bootstrap.connect(host, port).sync().channel()
  private val pipeline = channel.pipeline()
  private val emptyPromise = channel.voidPromise()

  def send(data: DataType, receiver: Observer[RespType]): Unit = {
    pipeline.write(RxAdapter.writeAndFlush(data, receiver), emptyPromise)
  }

  val closed: Observable[Unit] = closedSubject

  def close(): Observable[Unit] = {
    val closingSubject = AsyncSubject.create[Unit]()
    channel.close.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture): Unit = {
        closedSubject.onCompleted()
        try {
          if (future.isSuccess) {
            closingSubject.onCompleted()
          } else {
            closingSubject.onError(future.cause)
          }
        } finally {
          bootstrap.group.shutdownGracefully
        }
      }
    })
    closingSubject
  }
}

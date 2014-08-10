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

import rx.Observer
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ ChannelFuture, ChannelFutureListener, ChannelOption }
import io.netty.util.concurrent.DefaultThreadFactory

import rx.redis.pipeline.{ AdapterAction, RxChannelInitializer }
import rx.redis.resp.{ DataType, RespType }

import scala.language.implicitConversions

private[redis] class RxOnNettyClient(host: String, port: Int) extends NettyClient {
  @inline
  private final implicit def writeToRunnable(f: ⇒ ChannelFuture): Runnable = new Runnable {
    def run(): Unit = f
  }

  private final val channelInitializer =
    new RxChannelInitializer(optimizeForThroughput = true)

  private final val threadFactory =
    new DefaultThreadFactory("rx-redis", true)

  private final val eventLoopGroup =
    new NioEventLoopGroup(1, threadFactory)

  private final val bootstrap = {
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

  private final val channel = bootstrap.connect(host, port).sync().channel()
  private final val pipeline = channel.pipeline()
  private final val emptyPromise = channel.voidPromise()

  def send(data: DataType, receiver: Observer[RespType]): Unit = {
    eventLoopGroup.execute(pipeline.write(AdapterAction.writeAndFlush(data, receiver), emptyPromise))
  }

  def buffer(data: DataType, receiver: Observer[RespType]): Unit = {
    eventLoopGroup.execute(pipeline.write(AdapterAction.write(data, receiver), emptyPromise))
  }

  def flush(): ChannelFuture = {
    val promise = channel.newPromise()
    eventLoopGroup.execute(pipeline.write(AdapterAction.flush(), emptyPromise))
    promise
  }

  def close(): ChannelFuture = {
    val p = channel.close()
    p.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture): Unit = {
        bootstrap.group.shutdownGracefully
      }
    })
    p
  }
}

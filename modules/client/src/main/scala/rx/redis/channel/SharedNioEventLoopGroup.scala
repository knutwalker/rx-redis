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

package rx.redis.channel

import java.util.Spliterator
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ TimeUnit, ThreadFactory }

import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.ReferenceCounted
import io.netty.util.concurrent.{ EventExecutor, GlobalEventExecutor, DefaultPromise, Future }

import java.util.function.Consumer

final class SharedNioEventLoopGroup(threadCount: Int, threadFactory: ThreadFactory) extends NioEventLoopGroup(threadCount, threadFactory) with ReferenceCounted {

  private val refs = new AtomicInteger(1)
  private def newFuture = new DefaultPromise[Void](GlobalEventExecutor.INSTANCE)

  sys.addShutdownHook {
    refs.set(0)
    shutdownGracefully().syncUninterruptibly()
  }

  override def shutdownGracefully(quietPeriod: Long, timeout: Long, unit: TimeUnit): Future[_] = {
    if (release()) {
      super.shutdownGracefully(quietPeriod, timeout, unit)
    } else {
      newFuture.setSuccess(null)
    }
  }

  @deprecated("use shutdownGracefully", "Netty 4.x")
  override def shutdown(): Unit = {
    if (release()) {
      super.shutdown()
    }
  }

  def refCnt(): Int =
    refs.get()

  def retain(): SharedNioEventLoopGroup = {
    refs.incrementAndGet()
    this
  }

  def retain(increment: Int): SharedNioEventLoopGroup = {
    refs.addAndGet(increment)
    this
  }

  def release(): Boolean = {
    refs.decrementAndGet() <= 0
  }

  def release(decrement: Int): Boolean = {
    refs.addAndGet(-decrement) <= 0
  }

  // IntelliJ complains otherwise
  override def forEach(action: Consumer[_ >: EventExecutor]): Unit = super.forEach(action)
  override def spliterator(): Spliterator[EventExecutor] = super.spliterator()
}

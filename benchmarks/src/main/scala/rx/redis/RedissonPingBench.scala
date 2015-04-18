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

package rx.redis

import rx.redis.util._

import com.lambdaworks.redis.{ RedisAsyncConnection, RedisClient, RedisConnection }
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.concurrent
import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit

@Threads(value = 1)
@Fork(value = 1)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class RedissonPingBench {

  @Benchmark
  @OperationsPerInvocation(10000)
  def async_10000(s: RedissonPingBench.AsyncState): Boolean = {
    (1 until 10000).foreach(_ ⇒ s.conn.ping())
    s.conn.ping().await(1, TimeUnit.MINUTES)
  }

  @Benchmark
  @OperationsPerInvocation(100000)
  def async_100000(s: RedissonPingBench.AsyncState): Boolean = {
    (1 until 100000).foreach(_ ⇒ s.conn.ping())
    s.conn.ping().await(1, TimeUnit.MINUTES)
  }

  @Benchmark
  def sync(s: RedissonPingBench.SyncState): String =
    s.conn.ping()
}
object RedissonPingBench {

  @State(Scope.Benchmark)
  class SyncState {

    private[this] final var eventLoopGroup: EventLoopGroup = _
    private[this] final var client: RedisClient = _
    private[this] final var syncConnection: RedisConnection[String, String] = _

    def conn: RedisConnection[String, String] = syncConnection

    @Setup(Level.Iteration)
    def prepare(): Unit = {
      eventLoopGroup = new NioEventLoopGroup()
      client = new RedisClient(eventLoopGroup, DefaultRedisHost, DefaultRedisPort, 60000)
      syncConnection = client.connect()
    }

    @TearDown(Level.Iteration)
    def check(): Unit = {
      syncConnection.close()
      client.shutdown()
      eventLoopGroup.shutdownGracefully().sync().get()
    }
  }

  @State(Scope.Benchmark)
  class AsyncState {

    private[this] final var eventLoopGroup: EventLoopGroup = _
    private[this] final var client: RedisClient = _
    private[this] final var asyncConnection: RedisAsyncConnection[String, String] = _

    def conn: RedisAsyncConnection[String, String] = asyncConnection

    @Setup(Level.Iteration)
    def prepare(): Unit = {
      eventLoopGroup = new NioEventLoopGroup()
      client = new RedisClient(eventLoopGroup, DefaultRedisHost, DefaultRedisPort, 60000)
      asyncConnection = client.connectAsync()
    }

    @TearDown(Level.Iteration)
    def check(): Unit = {
      asyncConnection.close()
      client.shutdown()
      eventLoopGroup.shutdownGracefully().sync().get()
    }
  }
}

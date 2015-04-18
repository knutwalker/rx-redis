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

package com.twitter.finagle.redis

import rx.redis.util._

import com.twitter.finagle.redis.protocol._
import com.twitter.finagle.redis.util.StringToChannelBuffer
import com.twitter.finagle.{ Redis ⇒ RedisClient }
import com.twitter.util.{ Await, Future }
import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit

@Threads(value = 1)
@Fork(value = 1)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class FinaglePingBench {

  private[this] final var client: Client = _

  @Setup(Level.Iteration)
  def prepare(): Unit = {
    client = RedisClient.newRichClient(s"$DefaultRedisHost:$DefaultRedisPort")
  }

  @TearDown(Level.Iteration)
  def check(): Unit = {
    Await.result(client.release())
  }

  @Benchmark
  @OperationsPerInvocation(100000)
  def async_100000(): String = {
    (1 until 100000).foreach(_ ⇒ FinaglePingBench.ping(client))
    Await.result(FinaglePingBench.ping(client))
  }

  @Benchmark
  @OperationsPerInvocation(10000)
  def async_10000(): String = {
    (1 until 10000).foreach(_ ⇒ FinaglePingBench.ping(client))
    Await.result(FinaglePingBench.ping(client))
  }

  @Benchmark
  def sync(): String =
    Await.result(FinaglePingBench.ping(client))
}
object FinaglePingBench {
  object Ping extends Command {
    val command = "PING"
    val toChannelBuffer = RedisCodec.toUnifiedFormat(Seq(StringToChannelBuffer("PING")))
  }

  def ping(client: Client): Future[String] = client.doRequest(Ping) {
    case StatusReply(message) ⇒ Future.value(message)
    case BulkReply(message)   ⇒ Future.value(message.toString(Utf8))
  }
}

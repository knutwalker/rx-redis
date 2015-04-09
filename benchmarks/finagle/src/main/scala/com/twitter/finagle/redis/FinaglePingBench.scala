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
  def async(): Future[String] =
    client.doRequest(FinaglePingBench.Ping) {
      case BulkReply(message) ⇒ Future.value(message.toString(Utf8))
    }

  @Benchmark
  def sync(): String =
    Await.result(client.doRequest(FinaglePingBench.Ping) {
      case StatusReply(message) ⇒ Future.value(message)
    })
}
object FinaglePingBench {
  object Ping extends Command {
    val command = "PING"
    val toChannelBuffer = RedisCodec.toUnifiedFormat(Seq(StringToChannelBuffer("PING")))
  }
}

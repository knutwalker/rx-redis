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

import rx.Observable

import org.openjdk.jmh.annotations._
import rx.redis.clients.RawClient
import rx.redis.util._

import java.util.concurrent.TimeUnit

@Threads(value = 1)
@Fork(value = 1)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class RxPingBench {

  private[this] final var client: RawClient = _

  @Setup(Level.Iteration)
  def prepare(): Unit = {
    client = RawClient(DefaultRedisHost, DefaultRedisPort)
  }

  @TearDown(Level.Iteration)
  def check(): Unit = {
    client.disconnect().toBlocking.lastOrDefault(())
  }

  @Benchmark
  @OperationsPerInvocation(100000)
  def async_100000(): String = {
    (1 until 100000).foreach(_ ⇒ client.ping())
    client.ping().toBlocking.single()
  }

  @Benchmark
  @OperationsPerInvocation(10000)
  def async_10000(): String = {
    (1 until 10000).foreach(_ ⇒ client.ping())
    client.ping().toBlocking.single()
  }

  @Benchmark
  def sync(): String = {
    client.ping().toBlocking.single()
  }
}

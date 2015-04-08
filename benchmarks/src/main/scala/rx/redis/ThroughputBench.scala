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

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import rx.redis.clients.RawClient

import java.util.concurrent.TimeUnit

@Threads(value = 1)
@Fork(value = 1)
@Warmup(iterations = 3)
@Measurement(iterations = 5, time = 5)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class ThroughputBench {

  private[this] final var client: RawClient = _

  @Setup(Level.Iteration)
  def prepare(): Unit = {
    client = RawClient(util.DefaultRedisHost, util.DefaultRedisPort)
    client.command(cmd"FLUSHDB").toBlocking.single()
  }

  @TearDown(Level.Iteration)
  def check(): Unit = {
    val foo: Long = client.get[Long]("foo").toBlocking.single().getOrElse(-1L)
    assert(foo > 0, "Nothing changed?")
    client.disconnect().toBlocking.lastOrDefault(())
  }

  @Benchmark
  @OperationsPerInvocation(100000)
  def _01_async(bh: Blackhole): Unit = {
    bh.consume((1 to 100000).foreach(_ ⇒ client.incr("foo")))
  }

  @Benchmark
  @OperationsPerInvocation(100000)
  def _02_asyncWithFlush(bh: Blackhole): Unit = {
    (1 until 100000).foreach(_ ⇒ client.incr("foo"))
    client.incr("foo").toBlocking.single()
  }

  @Benchmark
  @OperationsPerInvocation(10000)
  def _03_sync(bh: Blackhole): Unit = {
    bh.consume((1 to 10000).foreach(_ ⇒ client.incr("foo").toBlocking.single()))
  }
}

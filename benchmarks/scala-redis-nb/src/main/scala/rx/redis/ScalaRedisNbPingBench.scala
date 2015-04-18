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

import util._

import akka.actor.ActorSystem
import akka.util.Timeout
import com.redis.RedisClient
import akka.pattern.gracefulStop
import com.redis.protocol.ConnectionCommands.Ping
import concurrent.{ Future, Await }
import scala.concurrent.duration._
import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit

@Threads(value = 1)
@Fork(value = 1)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class ScalaRedisNbPingBench {

  //  private[this] implicit final val ec = system.dispatcher
  private[this] implicit final val timeout = Timeout(1.hour)

  private[this] implicit final var system: ActorSystem = _
  private[this] final var client: RedisClient = _

  @Setup(Level.Iteration)
  def prepare(): Unit = {
    system = ActorSystem("redis")
    client = RedisClient(DefaultRedisHost, DefaultRedisPort)
  }

  @TearDown(Level.Iteration)
  def check(): Unit = {
    Await.result(gracefulStop(client.clientRef, 1.hour), Duration.Inf)
    system.shutdown()
  }

  @Benchmark
  @OperationsPerInvocation(100000)
  def async_100000(): Boolean = {
    (1 until 100000).foreach(_ ⇒ client.ping())
    Await.result(client.ping(), Duration.Inf)
  }

  @Benchmark
  @OperationsPerInvocation(10000)
  def async_10000(): Boolean = {
    (1 until 10000).foreach(_ ⇒ client.ping())
    Await.result(client.ping(), Duration.Inf)
  }

  @Benchmark
  def sync(): Boolean =
    Await.result(client.ping(), Duration.Inf)
}

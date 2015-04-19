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

package rx.redis

import rx.redis.clients.RawClient
import rx.redis.resp.{RespBytes, RespType}
import rx.redis.serialization._
import rx.redis.util.{DefaultRedisHost, DefaultRedisPort, Utf8}

import rx.functions.Action1

import io.netty.buffer.{ByteBuf, Unpooled}
import org.scalatest.FunSuite


class ThreadSafetySpec extends FunSuite {

  val total = 10000
  val threadCount = 8

  private class TestThread(
      commandToSend: () ⇒ ByteBuf,
      expected: RespType,
      getClient: => RawClient,
      shutdown: RawClient => Unit)
    extends Thread {

    private final var _correct = 0
    private final var _incorrect = 0
    private final val action: Action1[_ >: RespType] =
      new Action1[RespType] {
        def call(t1: RespType): Unit = {
          if (t1 == expected) _correct +=1
          else _incorrect += 1
        }
      }

    private final val client = getClient

    override def run(): Unit = {
      for (n <- 1 until total) {
        client.command(commandToSend()).forEach(action)
      }
      val last = client.command(commandToSend())
      last.toBlocking.forEach(action)

      shutdown(client)
    }

    def incorrect: Int = _incorrect
    def correct: Int = _correct
  }

  test("must not diverge when shared amongst threads") {

    val client = RawClient(DefaultRedisHost, DefaultRedisPort)

    def createThread(n: Int) = new TestThread(
      () ⇒ cmd"ECHO ${n.toString}",
      RespBytes(n.toString),
      client,
      _ => ())

    val threads = List.tabulate(threadCount)(createThread)

    threads foreach (_.start())
    threads foreach (_.join())

    threads foreach { t =>
      assert(t.correct == total)
      assert(t.incorrect == 0)
    }
  }

  test("must not diverge when used in isolation") {

    def client = RawClient(DefaultRedisHost, DefaultRedisPort)

    def createThread(n: Int) = new TestThread(
      () ⇒ cmd"ECHO ${n.toString}",
      RespBytes(n.toString),
      client,
      _.disconnect().toBlocking.lastOrDefault(()))

    val threads = List.tabulate(threadCount)(createThread)

    threads foreach (_.start())
    threads foreach (_.join())

    threads foreach { t =>
      assert(t.correct == total)
      assert(t.incorrect == 0)
    }
  }

  def RespBytes(s: String): RespBytes =
    rx.redis.resp.RespBytes.wrap(Unpooled.copiedBuffer(s, Utf8))
}

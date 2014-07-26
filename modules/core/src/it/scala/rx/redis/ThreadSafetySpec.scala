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

import org.scalatest.FunSuite
import rx.functions.Action1
import rx.redis.client.RawClient
import rx.redis.resp.{DataType, RespBytes, RespType}


class ThreadSafetySpec extends FunSuite {

  val total = 4000
  val threadCount = 25

  private class TestThread(
      commandToSend: DataType,
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
        client.command(commandToSend).forEach(action)
      }
      val last = client.command(commandToSend)
      last.toBlocking.forEach(action)

      shutdown(client)
    }

    def incorrect: Int = _incorrect
    def correct: Int = _correct
  }

  test("must not diverge when marked as shareable and shared amongst threads") {

    val client = RawClient("127.0.0.1", 6379, shareable = true)

    def createThread(n: Int) = new TestThread(cmd"ECHO ${n.toString}", RespBytes(n.toString), client, _ => ())

    val threads = List.tabulate(threadCount)(createThread)

    threads foreach (_.start())
    threads foreach (_.join())

    threads foreach { t =>
      assert(t.correct == total)
      assert(t.incorrect == 0)
    }
  }

  test("must not diverge when not shareable, but used in isolation") {

    def client = RawClient("127.0.0.1", 6379, shareable = false)

    def createThread(n: Int) = new TestThread(cmd"ECHO ${n.toString}", RespBytes(n.toString), client, { c =>
      c.shutdown()
      c.closedObservable.toBlocking.lastOrDefault(())
    })

    val threads = List.tabulate(threadCount)(createThread)

    threads foreach (_.start())
    threads foreach (_.join())

    threads foreach { t =>
      assert(t.correct == total)
      assert(t.incorrect == 0)
    }
  }

  test("should diverge when not marked as shareable but shared amongst threads") {

    val client = RawClient("127.0.0.1", 6379, shareable = false)

    def createThread(n: Int) = new TestThread(cmd"ECHO ${n.toString}", RespBytes(n.toString), client, _ => ())

    val threads = List.tabulate(threadCount)(createThread)

    threads foreach (_.start())
    threads foreach (_.join())

    threads foreach { t =>
      if (t.correct == total && t.incorrect == 0) {
        info("Thread did not diverge, lucky you.")
      }
    }
  }
}

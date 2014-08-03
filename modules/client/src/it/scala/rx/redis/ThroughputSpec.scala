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

import org.scalatest.{BeforeAndAfter, FunSuite}

class ThroughputSpec extends FunSuite with BeforeAndAfter {

  private def measure(repetitions: Int)(f: RawClient => Unit) = {
    val client = RawClient("127.0.0.1", 6379)
    client.command(cmd"FLUSHDB").toBlocking.single()
    val start = System.currentTimeMillis()

    f(client)

    val afterTest = System.currentTimeMillis()

    client.shutdown()
    client.closedObservable.toBlocking.lastOrDefault(())

    val end = System.currentTimeMillis()

    val took = afterTest - start
    val shutdown = end - afterTest
    (took / 1000.0, shutdown / 1000.0)
  }

  private def naiveBench(label: String, reqPerSec: Double)(f: Int => RawClient => Unit) = {
    val maxSecs = 10.0
    val total = (reqPerSec * maxSecs).toInt
    val (took, shutdown) = measure(total)(f(total))
    note(f"$label, $total times took $took%.2f s | ${total / took}%.2f Req/s")
    note(f"Waiting for shutdown took $shutdown%.2f s >>> ${total / (took + shutdown)}%.2f Req/s")
    assert(took <= maxSecs)
    total
  }

  private def getFoo = {
    val client = RawClient("127.0.0.1", 6379)
    val single = client.get[Long]("foo").toBlocking.single()
    client.shutdown()
    client.closedObservable.toBlocking.lastOrDefault(())
    single.getOrElse(-1L)
  }

  test("async sending should be >100k/s") {
    val total = naiveBench("Async Sending w/o waiting", 1e5) { total => client =>
      (1 to total).foreach(_ => client.incr("foo"))
    }
    assert(getFoo == total)
  }

  test("sending async, but waiting for the responses should be >100k/s") {
    val total = naiveBench("Async Sending with blocking on last", 1e5) { total => client =>
      (1 until total).foreach(_ => client.incr("foo"))
      client.incr("foo").toBlocking.single()
    }
    assert(getFoo == total)
  }

  test("sending all sync should be >10k/s") {
    val total = naiveBench("Async Sending with blocking on last", 1e4) { total => client =>
      (1 to total).foreach(_ => client.incr("foo").toBlocking.single())
    }
    assert(getFoo == total)
  }
}

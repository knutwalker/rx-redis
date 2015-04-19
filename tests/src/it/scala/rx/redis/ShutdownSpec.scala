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
import rx.redis.util.{DefaultRedisHost, DefaultRedisPort}
import serialization.ByteBufReader

import org.scalatest.{BeforeAndAfter, FunSuite}

class ShutdownSpec extends FunSuite with BeforeAndAfter {

  private def execute(f: RawClient => Unit) = {
    val client = RawClient(DefaultRedisHost, DefaultRedisPort)
    client.command(cmd"FLUSHDB").toBlocking.single()
    f(client)
    client.disconnect().toBlocking.lastOrDefault(())
  }

  private def getFoo = {
    val client = RawClient(DefaultRedisHost, DefaultRedisPort)
    val single = client.get("foo")(ByteBufReader.readLongAsString).toBlocking.single()
    client.disconnect().toBlocking.lastOrDefault(())
    single.getOrElse(-1L)
  }

  test("async sending") {
    execute { client ⇒
      (1 to 100000).foreach(_ => client.incr("foo"))
    }
    assert(getFoo == 100000)
  }

  test("sending async, but waiting for the responses") {
    execute { client ⇒
      (1 until 100000).foreach(_ => client.incr("foo"))
      client.incr("foo").toBlocking.single()
    }
    assert(getFoo == 100000)
  }

  test("sending all sync") {
    execute { client ⇒
      (1 to 10000).foreach(_ => client.incr("foo").toBlocking.single())
    }
    assert(getFoo == 10000)
  }
}

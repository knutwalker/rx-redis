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

package rx.redis.commands.it
import scala.concurrent.duration._

class KeyCommandsSpec extends CommandsSuite {

  test("DEL") {
    client.mset("foo1" -> "bar", "foo2" -> "baz").synch

    assert(client.exists("foo1").synch)
    assert(client.exists("foo2").synch)

    client.del("foo1", "foo2").synch

    assert(!client.exists("foo1").synch)
    assert(!client.exists("foo2").synch)
  }

  test("EXISTS") {
    assert(!client.exists("foo").synch)
    client.set("foo", "bar")
    assert(client.exists("foo").synch)
  }

  test("EXPIRE") {
    client.set("foo", "bar")
    client.expire("foo", 2.seconds)
    Thread.sleep(1000)
    assert(client.exists("foo").synch)
    Thread.sleep(1001)
    assert(!client.exists("foo").synch)
  }

  test("EXPIRE AT") {
    client.set("foo", "bar")
    client.expireAt("foo", 2.seconds.fromNow)
    Thread.sleep(1000)
    assert(client.exists("foo").synch)
    Thread.sleep(1001)
    assert(!client.exists("foo").synch)
  }

  test("KEYS") {
    assert(client.keys("*").synchAll == Nil)

    val expected = List("foo", "bar", "baz", "qux", "fuo", "qax", "fo[o]")

    client.mset(expected.map(k => (k, 42L)): _*).synch

    assert(client.keys("*").synchAll.toSet == expected.toSet)
    assert(client.keys("f[uo]o").synchAll.toSet == Set("foo", "fuo"))
    assert(client.keys("ba?").synchAll.toSet == Set("bar", "baz"))
    assert(client.keys("*a*").synchAll.toSet == Set("bar", "baz", "qax"))
    assert(client.keys("fo\\[o\\]").synchAll.toSet == Set("fo[o]"))
  }

  test("RANDOMKEY") {
    assert(client.randomKey().synch == None)
    client.set("foo", "bar")

    assert(client.randomKey().synch == Some("foo"))

    val keys = Set("foo", "bar").map(Option(_))
    client.set("bar", "foo")
    assert(keys(client.randomKey().synch))
  }

  test("TTL") {
    assert(client.ttl("foo").synch == -2)
    client.set("foo", "bar")
    assert(client.ttl("foo").synch == -1)
    client.expire("foo", 2.seconds)
    assert(client.ttl("foo").synch == 2)
    Thread.sleep(1000)
    assert(client.ttl("foo").synch == 1)
    Thread.sleep(1001)
    assert(client.ttl("foo").synch == -2)
  }
}

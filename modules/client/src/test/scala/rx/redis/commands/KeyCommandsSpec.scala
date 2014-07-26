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

package rx.redis.commands

import rx.redis._
import scala.concurrent.duration._

class KeyCommandsSpec extends CommandsSuite {

  test("DEL") {
    val del = Del("foo", "bar", "baz")
    sers(del, "DEL", "foo", "bar", "baz")
    ser(del, cmd"DEL foo bar baz")
  }

  test("EXISTS") {
    val exists = Exists("foo")
    sers(exists, "EXISTS", "foo")
    ser(exists, cmd"EXISTS foo")
  }

  test("EXPIRE") {
    val expire = Expire("foo", 42 seconds)
    sers(expire, "EXPIRE", "foo", "42")
    ser(expire, cmd"EXPIRE foo 42")
  }

  test("EXPIREAT") {
    val in42Seconds = ((System.currentTimeMillis() + 42000) / 1000).toString
    val deadline = 42.seconds.fromNow
    val expireAt = ExpireAt("foo", deadline)
    sers(expireAt, "EXPIREAT", "foo", in42Seconds.toString)
    ser(expireAt, cmd"EXPIREAT foo $in42Seconds")
  }

  test("KEYS") {
    val keys = Keys("f*bar?")
    sers(keys, "KEYS", "f*bar?")
    ser(keys, cmd"KEYS f*bar?")
  }

  test("RANDOMKEY") {
    val randomKey = RandomKey
    sers(randomKey, "RANDOMKEY")
    ser(randomKey, cmd"RANDOMKEY")
  }

  test("TTL") {
    val ttl = Ttl("foo")
    sers(ttl, "TTL", "foo")
    ser(ttl, cmd"TTL foo")
  }

}

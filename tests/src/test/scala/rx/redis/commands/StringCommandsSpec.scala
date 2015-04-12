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

package rx.redis.commands

import scala.concurrent.duration._

import rx.redis._
import rx.redis.serialization._

class StringCommandsSpec extends CommandsSuite {

  test("GET") {
    val get = Get("foo")
    sers(get, "GET", "foo")
    ser(get, cmd"GET foo")

    val get2 = Get("foo\r\nbar")
    sers(get2, "GET", "foo\r\nbar")
    ser(get2, cmd"GET foo\r\nbar")
  }

  test("SET") {
    val set = Set("foo", "bar")
    sers(set, "SET", "foo", "bar")
    ser(set, cmd"SET foo bar")

    val set2 = Set("foo", "foo\r\nbar")
    sers(set2, "SET", "foo", "foo\r\nbar")
    ser(set2, cmd"SET foo foo\r\nbar")
  }

  test("SETEX") {
    val setEx = SetEx("foo", 13.seconds, "bar")
    sers(setEx, "SETEX", "foo", "13", "bar")
    ser(setEx, cmd"SETEX foo 13 bar")
  }

  test("SETNX") {
    val setNx = SetNx("foo", "bar")
    sers(setNx, "SETNX", "foo", "bar")
    ser(setNx, cmd"SETNX foo bar")
  }

  test("INCR") {
    val incr = Incr("foo")
    sers(incr, "INCR", "foo")
    ser(incr, cmd"INCR foo")
  }

  test("DECR") {
    val decr = Decr("foo")
    sers(decr, "DECR", "foo")
    ser(decr, cmd"DECR foo")
  }

  test("INCRBY") {
    val incrBy = IncrBy("foo", 42)
    sers(incrBy, "INCRBY", "foo", "42")
    ser(incrBy, cmd"INCRBY foo 42")
  }

  test("DECRBY") {
    val decrBy = DecrBy("foo", 42)
    sers(decrBy, "DECRBY", "foo", "42")
    ser(decrBy, cmd"DECRBY foo 42")
  }

  test("MGET") {
    val mget = MGet("foo", "bar", "baz")
    sers(mget, "MGET", "foo", "bar", "baz")
    ser(mget, cmd"MGET foo bar baz")
  }

  test("MSET") {
    val mset = MSet("foo" -> "bar", "bar" -> "baz")
    sers(mset, "MSET", "foo", "bar", "bar", "baz")
    ser(mset, cmd"MSET foo bar bar baz")
  }

  test("STRLEN") {
    val strLen = StrLen("foobar")
    sers(strLen, "STRLEN", "foobar")
    ser(strLen, cmd"STRLEN foobar")
  }

}

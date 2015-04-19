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

import rx.redis._
import serialization.ByteBufReader


class HashCommandsSpec extends ItCommandsSuite {

  implicit val frameless = ByteBufReader.readFramelessString

  test("HGET") {
    client.command(cmd"HSET foo bar baz")

    assert(client.hget[String]("foo", "bar").synch == Some("baz"))
    assert(client.hget[String]("foo", "baz").synch == None)
    assert(client.hget[String]("bar", "baz").synch == None)
  }

  test("HGETALL") {
    val m = Map("bar" -> "1", "baz" -> "2", "qux" -> "3")
    m foreach {
      case (k, v) => client.command(cmd"HSET foo $k $v")
    }

    val result = client.hgetAll[String]("foo").synchAll.toMap
    assert(result == m)

    assert(client.hgetAll[String]("bar").synchAll.isEmpty)
  }

}

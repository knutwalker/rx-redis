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

package com.example

import rx.redis.api.RxRedis


object Example extends App {

  val client = RxRedis("localhost", 6379)

  client.del("foo", "foo1", "foo2", "foo3", "foo4", "foo5", "what?").
    toBlocking.foreach(println)

  client.get("key")
  client.mset(
    "foo1" -> "foo1",
    "foo2" -> "foo2",
    "foo3" -> "foo3")

  val gets =
    client.get("foo") merge client.get("foo1")

  val mget =
    client.mget("foo1", "foo2", "foo4", "foo3", "foo5")

  gets.merge(mget).
    foreach(r => println("GET or MGET = " + r))

  val mixed = client.ping() ++
    client.echo("42") ++
    client.incr("what?").map(_.toString)

  mixed.foreach(r => println("mixed = " + r))

  RxRedis.shutdown(client)
}

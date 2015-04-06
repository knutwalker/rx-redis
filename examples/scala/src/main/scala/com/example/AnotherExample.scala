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

package com.example

import rx.redis._
import rx.redis.api.RxRedis

object AnotherExample extends App {

  val client = RxRedis("localhost", 6379)

  val INFO = cmd"INFO"
  val CLIENT_LIST = cmd"CLIENT LIST"

  val infoPart = "server"
  val SERVER_INFO = cmd"INFO $infoPart"

  client.ping().toBlocking.foreach { r ⇒
    println(s"first PING : $r")
  }

  println("after first PING")

  client.ping().toBlocking.foreach { r ⇒
    println(s"second PING : $r")
  }

  println("after second PING")

  client.command(INFO).foreach { r ⇒
    println(s"INFO : ${preview(r)}")
  }

  println("after INFO")

  client.ping().foreach { r ⇒
    println(s"third PING : $r")
  }

  println("after third PING")

  client.command(CLIENT_LIST).foreach { r ⇒
    println(s"CLIENT LIST : ${preview(r)}")
  }

  println("after CLIENT LIST")

  client.command(SERVER_INFO).foreach { r ⇒
    println(s"SERVER INFO: ${preview(r)}")
  }

  println("after SERVER INFO")

  client.set("bar", "foo")
  client.set("bazz", "oh noes")

  client.set("foo", "bar").foreach(println)

  client.mget("foo", "bar", "baz").foreach { r ⇒
    println(s"MGET: $r")
  }

  client.get("foo") foreach { getResult ⇒
    println(s"GET foo: $getResult")
  }

  println("before shutdown")

  RxRedis.shutdown(client)

  println("Client is closed")
}

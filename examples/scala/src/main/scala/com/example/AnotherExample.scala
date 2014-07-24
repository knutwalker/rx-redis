package com.example

import rx.redis._
import rx.redis.api.RxRedis


object AnotherExample extends App {

  val client = RxRedis("localhost", 6379)

  val INFO = cmd"INFO"
  val CLIENT_LIST = cmd"CLIENT LIST"

  val infoPart = "server"
  val SERVER_INFO = cmd"INFO $infoPart"

  client.ping().toBlocking.foreach { r =>
    println(s"first PING : $r")
  }

  println("after first PING")

  client.ping().toBlocking.foreach { r =>
    println(s"second PING : $r")
  }

  println("after second PING")

  client.command(INFO).foreach { r =>
    println(s"INFO : ${preview(r)}")
  }

  println("after INFO")

  client.ping().foreach { r =>
    println(s"third PING : $r")
  }

  println("after third PING")

  client.command(CLIENT_LIST).foreach { r =>
    println(s"CLIENT LIST : ${preview(r)}")
  }

  println("after CLIENT LIST")

  client.command(SERVER_INFO).foreach { r =>
    println(s"SERVER INFO: ${preview(r)}")
  }

  println("after SERVER INFO")

  client.set("bar", "foo")
  client.set("bazz", "oh noes")

  client.set("foo", "bar").foreach(println)


  client.mget("foo", "bar", "baz").foreach { r =>
    println(s"MGET: $r")
  }


  client.get("foo").doOnCompleted(client.shutdown()) foreach { getResult =>
    println(s"GET foo: $getResult")
  }

  println("before await")

  RxRedis.await(client)

  println("Client is closed")
}

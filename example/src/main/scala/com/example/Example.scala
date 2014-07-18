package com.example

import rx.redis.RxRedis
import rx.redis.util.{command, preview}


object Example extends App {

  val client = RxRedis("localhost", 6379)

  val PING = command("PING")
  val INFO = command("INFO")
  val CLIENT_LIST = command("CLIENT LIST")
  val SERVER_INFO = command("INFO SERVER")

  client.command(PING).toBlocking.foreach { r =>
    println(s"first PING : ${preview(r)}")
  }

  println("after first PING")

  client.command(PING).toBlocking.foreach { r =>
    println(s"second PING : ${preview(r)}")
  }

  println("after second PING")

  client.command(INFO).foreach { r =>
    println(s"INFO : ${preview(r)}")
  }

  println("after INFO")

  client.command(PING).foreach { r =>
    println(s"third PING : ${preview(r)}")
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

  client.set("foo", "bar")
  client.get("foo").foreach { r =>
    println(s"GET foo: ${preview(r)}")
    client.shutdown()
  }

  println("before await")

  RxRedis.await(client)

  println("Client is closed")
}

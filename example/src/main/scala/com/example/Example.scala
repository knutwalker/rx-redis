package com.example

import rx.lang.scala.JavaConversions._
import rx.lang.scala.XJavaConversions._

import rx.redis.RxRedis
import rx.redis.util._


object Example extends App {

  val client = RxRedis("localhost", 6379)

  val PING = cmd"PING"
  val INFO = cmd"INFO"
  val CLIENT_LIST = cmd"CLIENT LIST"

  val infoPart = "server"
  val SERVER_INFO = cmd"INFO $infoPart"

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

  val set = client.set("foo", "bar")
  val get = client.get("foo")

  set.merge(get).doOnCompleted(client.shutdown()) foreach { r =>
    println(s"GET foo: ${preview(r)}")
  }

  println("before await")

  RxRedis.await(client)

  println("Client is closed")
}

package com.example

import rx.redis.RxRedis


object Example extends App {

  val client = RxRedis("localhost", 6379)

  client.del("foo", "foo1", "foo2", "foo3", "foo4", "foo5", "what?").
    toBlocking.foreach(println)

  client.get("key")
  client.mset("foo1" -> "foo1", "foo2" -> "foo2", "foo3" -> "foo3")

  val gets = client.get("foo") merge client.get("foo1")
  val mget = client.mget("foo1", "foo2", "foo4", "foo3", "foo5")

  gets.merge(mget).foreach(r => println("GET or MGET = " + r))

  val mixed = client.ping() ++
    client.echo("42") ++
    client.incr("what?").map(_.toString)
  mixed.doOnCompleted(client.shutdown())
    .foreach(r => println("mixed = " + r))

  RxRedis.await(client)
}

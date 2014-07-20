package com.example

import rx.redis.RxRedis

object Throughput extends App {

  val client = RxRedis("127.0.0.1", 6379, shareable = false)

  val total = args.headOption.fold(1000000)(_.toInt)
  val start = System.currentTimeMillis()

  for (_ <- 1 until total) {
    client.ping()
  }

  val async = System.currentTimeMillis()

  client.ping().toBlocking.first()

  val end = System.currentTimeMillis()

  client.shutdown()
  RxRedis.await(client)

  val closed = System.currentTimeMillis()

  val tAsync = (async - start) / 1000.0
  val tEnd = (end - start) / 1000.0
  val tClosed = closed - end

  println(f"Sending ${total - 1} PINGs in $tAsync%.0f seconds | ${(total - 1) / tAsync}%.2f Req/s")
  println(f"Sending and receiving $total PINGs in $tEnd%.0f seconds | ${total / tEnd}%.2f Req/s")
  println(s"Closing took tClosed ms")
}

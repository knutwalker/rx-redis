package com.example

import rx.redis.RxRedis

object Throughput extends App {

  val total = args.headOption.fold(1000000)(_.toInt)
  
  def testAsync() = {
    val client = RxRedis("127.0.0.1", 6379)
    val start = System.currentTimeMillis()

    (1 to total).foreach(_ => client.ping())
    val took = System.currentTimeMillis() - start

    client.shutdown()
    RxRedis.await(client)
    
    took
  }

  def testParAsync() = {
    val client = RxRedis("127.0.0.1", 6379)
    val start = System.currentTimeMillis()

    (1 to total).par.foreach(_ => client.ping())
    val took = System.currentTimeMillis() - start

    client.shutdown()
    RxRedis.await(client)

    took
  }

  def testSyncAtLast() = {

    val client = RxRedis("127.0.0.1", 6379)
    val start = System.currentTimeMillis()

    (1 until total).foreach(_ => client.ping())

    client.ping().toBlocking.head
    
    val took = System.currentTimeMillis() - start

    client.shutdown()
    RxRedis.await(client)

    took
  }
  
  def testSyncAll() = {

    val client = RxRedis("127.0.0.1", 6379)
    val start = System.currentTimeMillis()

    (1 to total).foreach(_ => client.ping().toBlocking.head)

    val took = System.currentTimeMillis() - start

    client.shutdown()
    RxRedis.await(client)

    took
  }
  
  val tAsync = testAsync() / 1000.0
  val tParAsync = testParAsync() / 1000.0
  val tSyncLast = testSyncAtLast() / 1000.0
  val tSyncAll = testSyncAll() / 1000.0
  
  println(f"Async: ${total} PINGs took $tAsync%.0f s | ${total / tAsync}%.2f Req/s")
  println(f"Par Async: ${total} PINGs took $tParAsync%.0f s | ${total / tParAsync}%.2f Req/s")
  println(f"Sync last: ${total} PINGs took $tSyncLast%.0f s | ${total / tSyncLast}%.2f Req/s")
  println(f"Sync all: ${total} PINGs took $tSyncAll%.0f s | ${total / tSyncAll}%.2f Req/s")
}

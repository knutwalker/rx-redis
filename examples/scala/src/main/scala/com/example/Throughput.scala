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

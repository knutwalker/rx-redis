package com.example

import rx.redis.RxRedis
import rx.redis.resp.{DataType, RespType}

object MultiThreadedSharedClient extends App {

  val repetitions = args.headOption.fold(1000)(_.toInt)

  val client = RxRedis("localhost", 6379, shareable = true)

  def newThread(commandExpected: (DataType, RespType)): RedisThread = {
    new RedisThread(repetitions, commandExpected._1, commandExpected._2, client, _ => ())
  }

  val threads =  rrs map newThread

  doMultiThreaded(repetitions, threads)

  client.shutdown()
  RxRedis.await(client)
}

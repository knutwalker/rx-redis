package com.example

import rx.redis.api.RxRedis
import rx.redis.resp.{DataType, RespType}


object MultiThreadedClientPerThread extends App {

  val repetitions = args.headOption.fold(1000)(_.toInt)

  def newThread(commandExpected: (DataType, RespType)): RedisThread = {
    new RedisThread(repetitions, commandExpected._1, commandExpected._2, RxRedis("localhost", 6379, shareable = false), { client =>
      client.shutdown()
      RxRedis.await(client)
    })
  }

  val threads =  rrs map newThread

  doMultiThreaded(repetitions, threads)
}

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

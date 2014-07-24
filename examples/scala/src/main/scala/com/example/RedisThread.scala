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

import rx.lang.scala.Observable

import rx.redis.api.Client
import rx.redis.resp.{RespType, DataType}

class RedisThread(total: Int, command: DataType, expected: RespType, mkClient: => Client, shutdown: Client => Unit) extends Thread {
  private final var _correct = 0
  private final var _incorrect = 0
  private final val action = (r: RespType) => {
    if (r == expected) _correct +=1
    else {
      _incorrect += 1
      println(preview(r) + "  VS.  " + preview(expected))
    }
  }

  val client = mkClient

  override def run(): Unit = {
    for (n <- 1 until total) {
      client.command(command).foreach(action)
    }
    val observable: Observable[RespType] = client.command(command)
    observable.toBlocking.foreach(action)
    shutdown(client)
  }

  def incorrect: Int = _incorrect
  def correct: Int = _correct
}

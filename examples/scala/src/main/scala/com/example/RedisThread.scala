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

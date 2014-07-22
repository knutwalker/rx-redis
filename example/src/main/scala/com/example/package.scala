package com

import rx.redis._
import rx.redis.resp.{DataType, RespBytes, RespString, RespType}

package object example {

  val preview = (r: RespType) => r.toString.replaceAllLiterally("\r\n", "\\r\\n").take(50)

  val rrs: List[(DataType, DataType)] = List(
    cmd"PING" -> RespString("PONG"),
    cmd"ECHO foo" -> RespBytes("foo"),
    cmd"ECHO bar" -> RespBytes("bar"),
    cmd"ECHO baz" -> RespBytes("baz"),
    cmd"ECHO qux" -> RespBytes("qux"),
    cmd"ECHO foobar" -> RespBytes("foobar"),
    cmd"ECHO barbaz" -> RespBytes("barbaz"),
    cmd"ECHO quxall" -> RespBytes("quxall"),
    cmd"ECHO miau" -> RespBytes("miau")
  )

  val threadCount = rrs.size

  def doMultiThreaded(repetitions: Int, threads: List[RedisThread]) = {

    val start = System.currentTimeMillis()

    threads foreach (_.start())
    threads foreach (_.join())

    val end = System.currentTimeMillis()

    val took = (end - start).toDouble
    val requestCount = threadCount * repetitions
    println(f"finished sending ${requestCount} commands in ${took} ms. That is ${requestCount / took * 1000}%.2f Req/s")
    println("Erroneous threads (if any): ")
    threads filter (_.incorrect > 0) foreach { t =>
      println(s"Thread: ${t.getName}")
      println(s"  Correct: ${t.correct}")
      println(s"  Incorrect: ${t.incorrect}")
    }
  }
}

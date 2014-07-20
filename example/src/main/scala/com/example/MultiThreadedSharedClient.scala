package com.example

import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable

import rx.redis.RxRedis
import rx.redis.api.Client
import rx.redis.resp.{DataType, RespBytes, RespString, RespType}
import rx.redis.util._

object MultiThreadedSharedClient extends App {

  class MyThread(client: Client, total: Int, command: DataType, expected: RespType, f: RespType => Unit) extends Thread {
    private final var _correct = 0
    private final var _incorrect = 0
    private final val action = (r: RespType) => {
      if (r == expected) _correct +=1
      else _incorrect += 1
      f(r)
    }
    override def run(): Unit = {
      for (n <- 1 to (total - 1)) {
        client.command(command).foreach(action)
      }
      val observable: Observable[RespType] = client.command(command)
      observable.toBlocking.foreach(action)
    }

    def incorrect: Int = _incorrect
    def correct: Int = _correct
  }

  val client = RxRedis("localhost", 6379, shareable = true)
//  shareable is true by default
//  val client = RxRedis("localhost", 6379, shareable = true)

  val rrs: List[(DataType, DataType)] = List(
    resp"PING" -> RespString("PONG"),
    resp"ECHO foo" -> RespBytes("foo"),
    resp"ECHO bar" -> RespBytes("bar"),
    resp"ECHO baz" -> RespBytes("baz"),
    resp"ECHO qux" -> RespBytes("qux"),
    resp"ECHO foobar" -> RespBytes("foobar"),
    resp"ECHO barbaz" -> RespBytes("barbaz"),
    resp"ECHO quxall" -> RespBytes("quxall"),
    resp"ECHO miau" -> RespBytes("miau")
  )

  val repetitions = args(0).toInt

  val threadCount = rrs.size

  val threads = rrs map {
    case (cmd, res) => new MyThread(client, repetitions, cmd, res, r => {
//      if (res != r) println(preview(r) + "  VS.  " + preview(res))
    })
  }

  val start = System.currentTimeMillis()

  threads foreach (_.start())
  threads foreach (_.join())

  val end = System.currentTimeMillis()

  private val took = (end - start).toDouble
  private val requestCount = threadCount * repetitions
  println(f"finished sending ${requestCount} commands in ${took} ms. That is ${requestCount / took * 1000}%.2f Req/s")
  println("Erroneous threads (if any): ")
  threads filter (_.incorrect > 0) foreach { t =>
    println(s"Thread: ${t.getName}")
    println(s"  Correct: ${t.correct}")
    println(s"  Incorrect: ${t.incorrect}")
  }

  client.shutdown()
  RxRedis.await(client)
}

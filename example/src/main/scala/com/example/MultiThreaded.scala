package com.example

import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable

import rx.redis.api.Client
import rx.redis.resp.{DataType, RespBytes, RespString, RespType}
import rx.redis.serialization.Writes
import rx.redis.RxRedis
import rx.redis.util._

object MultiThreaded extends App {

  class MyThread[A: Writes](client: Client, total: Int, command: A, expected: RespType, f: RespType => Unit) extends Thread {
    private final var _correct = 0
    private final var _incorrect = 0
    override def run(): Unit = {
      for (n <- 1 to total) {
        val observable: Observable[RespType] = client.command(command)
        observable.toBlocking.foreach { r =>
          if (r == expected) _correct +=1
          else _incorrect += 1
          f(r)
        }
      }
    }

    def incorrect = _incorrect
    def correct = _correct
  }

  val client = RxRedis("localhost", 6379)

  val rrs: List[(String, DataType)] = List(
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

  val threads = rrs map {
    case (cmd, res) => new MyThread(client, args(0).toInt, cmd, res, r => {
      if (res != r) println(preview(r) + "  VS.  " + preview(res))
    })
  }

  threads foreach (_.start())
  threads foreach (_.join())

  println("Erroneous threads: ")
  threads filter (_.incorrect > 0) foreach { t =>
    println(s"Thread: ${t.getName}")
    println(s"  Correct: ${t.correct}")
    println(s"  Incorrect: ${t.incorrect}")
  }

  client.shutdown()
  RxRedis.await(client)
}

package com.example

import rx.redis.api.{Client, Writes}
import rx.redis.resp.{DataType, RespBytes, RespString, RespType}
import rx.redis.{RxRedis, util}

object MultiThreaded extends App {

  class MyThread[A: Writes](client: Client, total: Int, command: A, expected: RespType, f: RespType => Unit) extends Thread {
    private final var _correct = 0
    private final var _incorrect = 0
    override def run(): Unit = {
      for (n <- 1 to total) {
        client.command(command).toBlocking.foreach { r =>
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
    util.command("PING") -> RespString("PONG"),
    util.command("ECHO foo") -> RespBytes("foo"),
    util.command("ECHO bar") -> RespBytes("bar"),
    util.command("ECHO baz") -> RespBytes("baz"),
    util.command("ECHO qux") -> RespBytes("qux"),
    util.command("ECHO foobar") -> RespBytes("foobar"),
    util.command("ECHO barbaz") -> RespBytes("barbaz"),
    util.command("ECHO quxall") -> RespBytes("quxall"),
    util.command("ECHO miau") -> RespBytes("miau")
  )

  val threadCount = rrs.size

  val threads = rrs map {
    case (cmd, res) => new MyThread(client, args(0).toInt, cmd, res, r => {
      if (res != r) println(util.preview(r) + "  VS.  " + util.preview(res))
    })
  }

  threads foreach (_.start())
  threads foreach (_.join())

  threads foreach { t =>
    println(s"Thread: ${t.getName}")
    println(s"  Correct: ${t.correct}")
    println(s"  Incorrect: ${t.incorrect}")
  }

  client.shutdown()
  RxRedis.await(client)
}

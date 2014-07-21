package rx.redis.commands

import rx.redis.util._
import scala.concurrent.duration._

class StringCommandsSpec extends CommandsSuite {

  test("GET") {
    val get = Get("foo")
    cmds(get, "GET", "foo")
    cmd(get, resp"GET foo")

    val get2 = Get("foo\r\nbar")
    cmds(get2, "GET", "foo\r\nbar")
    cmd(get2, resp"GET foo\r\nbar")
  }

  test("SET") {
    val set = Set("foo", "bar")
    cmds(set, "SET", "foo", "bar")
    cmd(set, resp"SET foo bar")

    val set2 = Set("foo", "foo\r\nbar")
    cmds(set2, "SET", "foo", "foo\r\nbar")
    cmd(set2, resp"SET foo foo\r\nbar")
  }

  test("SETEX") {
    val setEx = SetEx("foo", 13.seconds, "bar")
    cmds(setEx, "SETEX", "foo", "13", "bar")
    cmd(setEx, resp"SETEX foo 13 bar")
  }

  test("SETNX") {
    val setNx = SetNx("foo", "bar")
    cmds(setNx, "SETNX", "foo", "bar")
    cmd(setNx, resp"SETNX foo bar")
  }

  test("INCR") {
    val incr = Incr("foo")
    cmds(incr, "INCR", "foo")
    cmd(incr, resp"INCR foo")
  }

  test("DECR") {
    val decr = Decr("foo")
    cmds(decr, "DECR", "foo")
    cmd(decr, resp"DECR foo")
  }

  test("INCRBY") {
    val incrBy = IncrBy("foo", 42)
    cmds(incrBy, "INCRBY", "foo", "42")
    cmd(incrBy, resp"INCRBY foo 42")
  }

  test("DECRBY") {
    val decrBy = DecrBy("foo", 42)
    cmds(decrBy, "DECRBY", "foo", "42")
    cmd(decrBy, resp"DECRBY foo 42")
  }

  test("MGET") {
    val mget = MGet("foo", "bar", "baz")
    cmds(mget, "MGET", "foo", "bar", "baz")
    cmd(mget, resp"MGET foo bar baz")
  }

  test("MSET") {
    val mset = MSet("foo" -> "bar", "bar" -> "baz")
    cmds(mset, "MSET", "foo", "bar", "bar", "baz")
    cmd(mset, resp"MSET foo bar bar baz")
  }

  test("STRLEN") {
    val strLen = StrLen("foobar")
    cmds(strLen, "STRLEN", "foobar")
    cmd(strLen, resp"STRLEN foobar")
  }

}

package rx.redis.commands

import rx.redis.util._
import scala.concurrent.duration._

class StringCommandsSpec extends CommandsSuite {

  test("GET") {
    val get = Get("foo")
    cmds(get, "*2", "$3", "GET", "$3", "foo")
    cmd(get, resp"GET foo")

    val get2 = Get("foo\r\nbar")
    cmds(get2, "*2", "$3", "GET", "$8", "foo\r\nbar")
    cmd(get2, resp"GET foo\r\nbar")
  }

  test("SET") {
    val set = Set("foo", "bar")
    cmds(set, "*3", "$3", "SET", "$3", "foo", "$3", "bar")
    cmd(set, resp"SET foo bar")

    val set2 = Set("foo", "foo\r\nbar")
    cmds(set2, "*3", "$3", "SET", "$3", "foo", "$8", "foo\r\nbar")
    cmd(set2, resp"SET foo foo\r\nbar")
  }

  test("SETEX") {
    val setEx = SetEx("foo", 13.seconds, "bar")
    cmds(setEx, "*4", "$5", "SETEX", "$3", "foo", "$2", "13", "$3", "bar")
    cmd(setEx, resp"SETEX foo 13 bar")
  }

  test("SETNX") {
    val setNx = SetNx("foo", "bar")
    cmds(setNx, "*3", "$5", "SETNX", "$3", "foo", "$3", "bar")
    cmd(setNx, resp"SETNX foo bar")
  }

  test("INCR") {
    val incr = Incr("foo")
    cmds(incr, "*2", "$4", "INCR", "$3", "foo")
    cmd(incr, resp"INCR foo")
  }

  test("DECR") {
    val decr = Decr("foo")
    cmds(decr, "*2", "$4", "DECR", "$3", "foo")
    cmd(decr, resp"DECR foo")
  }

  test("INCRBY") {
    val incrBy = IncrBy("foo", 42)
    cmds(incrBy, "*3", "$6", "INCRBY", "$3", "foo", "$2", "42")
    cmd(incrBy, resp"INCRBY foo 42")
  }

  test("DECRBY") {
    val decrBy = DecrBy("foo", 42)
    cmds(decrBy, "*3", "$6", "DECRBY", "$3", "foo", "$2", "42")
    cmd(decrBy, resp"DECRBY foo 42")
  }

  test("MGET") {
    val mget = MGet("foo", "bar", "baz")
    cmds(mget, "*4", "$4", "MGET", "$3", "foo", "$3", "bar", "$3", "baz")
    cmd(mget, resp"MGET foo bar baz")
  }

  test("MSET") {
    val mset = MSet("foo" -> "bar", "bar" -> "baz")
    cmds(mset, "*5", "$4", "MSET", "$3", "foo", "$3", "bar", "$3", "bar", "$3", "baz")
    cmd(mset, resp"MSET foo bar bar baz")
  }

  test("STRLEN") {
    val strLen = StrLen("foobar")
    cmds(strLen, "*2", "$6", "STRLEN", "$6", "foobar")
    cmd(strLen, resp"STRLEN foobar")
  }

}

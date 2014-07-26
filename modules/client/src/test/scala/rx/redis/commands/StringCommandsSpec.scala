package rx.redis.commands

import rx.redis._
import scala.concurrent.duration._

class StringCommandsSpec extends CommandsSuite {

  test("GET") {
    val get = Get("foo")
    sers(get, "GET", "foo")
    ser(get, cmd"GET foo")

    val get2 = Get("foo\r\nbar")
    sers(get2, "GET", "foo\r\nbar")
    ser(get2, cmd"GET foo\r\nbar")
  }

  test("SET") {
    val set = Set("foo", "bar")
    sers(set, "SET", "foo", "bar")
    ser(set, cmd"SET foo bar")

    val set2 = Set("foo", "foo\r\nbar")
    sers(set2, "SET", "foo", "foo\r\nbar")
    ser(set2, cmd"SET foo foo\r\nbar")
  }

  test("SETEX") {
    val setEx = SetEx("foo", 13.seconds, "bar")
    sers(setEx, "SETEX", "foo", "13", "bar")
    ser(setEx, cmd"SETEX foo 13 bar")
  }

  test("SETNX") {
    val setNx = SetNx("foo", "bar")
    sers(setNx, "SETNX", "foo", "bar")
    ser(setNx, cmd"SETNX foo bar")
  }

  test("INCR") {
    val incr = Incr("foo")
    sers(incr, "INCR", "foo")
    ser(incr, cmd"INCR foo")
  }

  test("DECR") {
    val decr = Decr("foo")
    sers(decr, "DECR", "foo")
    ser(decr, cmd"DECR foo")
  }

  test("INCRBY") {
    val incrBy = IncrBy("foo", 42)
    sers(incrBy, "INCRBY", "foo", "42")
    ser(incrBy, cmd"INCRBY foo 42")
  }

  test("DECRBY") {
    val decrBy = DecrBy("foo", 42)
    sers(decrBy, "DECRBY", "foo", "42")
    ser(decrBy, cmd"DECRBY foo 42")
  }

  test("MGET") {
    val mget = MGet("foo", "bar", "baz")
    sers(mget, "MGET", "foo", "bar", "baz")
    ser(mget, cmd"MGET foo bar baz")
  }

  test("MSET") {
    val mset = MSet("foo" -> "bar", "bar" -> "baz")
    sers(mset, "MSET", "foo", "bar", "bar", "baz")
    ser(mset, cmd"MSET foo bar bar baz")
  }

  test("STRLEN") {
    val strLen = StrLen("foobar")
    sers(strLen, "STRLEN", "foobar")
    ser(strLen, cmd"STRLEN foobar")
  }

}

package rx.redis.commands

import rx.redis.util._

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

  test("INCR") {
    val incr = Incr("foo")
    cmds(incr, "*2", "$4", "INCR", "$3", "foo")
    cmd(incr, resp"INCR foo")
  }

  test("INCRBY") {
    val incrBy = IncrBy("foo", 42)
    cmds(incrBy, "*3", "$6", "INCRBY", "$3", "foo", "$2", "42")
    cmd(incrBy, resp"INCRBY foo 42")
  }

  test("MGET") {
    val mget = MGet("foo", "bar", "baz")
    cmds(mget, "*4", "$4", "MGET", "$3", "foo", "$3", "bar", "$3", "baz")
    cmd(mget, resp"MGET foo bar baz")
  }

}

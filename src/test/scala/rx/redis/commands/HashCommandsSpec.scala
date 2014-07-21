package rx.redis.commands

import rx.redis.util._

class HashCommandsSpec extends CommandsSuite {

  test("HGET") {
    val hget = HGet("foo", "bar")
    cmds(hget, "HGET", "foo", "bar")
    cmd(hget, resp"HGET foo bar")
  }

  test("HGETALL") {
    val hgetAll = HGetAll("foo")
    cmds(hgetAll, "HGETALL", "foo")
    cmd(hgetAll, resp"HGETALL foo")
  }

}

package rx.redis.commands

import rx.redis.util._

class HashCommandsSpec extends CommandsSuite {

  test("HGET") {
    val hget = HGet("foo", "bar")
    cmds(hget, "*3", "$4", "HGET", "$3", "foo", "$3", "bar")
    cmd(hget, resp"HGET foo bar")
  }

  test("HGETALL") {
    val hgetAll = HGetAll("foo")
    cmds(hgetAll, "*2", "$7", "HGETALL", "$3", "foo")
    cmd(hgetAll, resp"HGETALL foo")
  }

}

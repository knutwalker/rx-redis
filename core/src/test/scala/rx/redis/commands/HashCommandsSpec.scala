package rx.redis.commands

import rx.redis._

class HashCommandsSpec extends CommandsSuite {

  test("HGET") {
    val hget = HGet("foo", "bar")
    sers(hget, "HGET", "foo", "bar")
    ser(hget, cmd"HGET foo bar")
  }

  test("HGETALL") {
    val hgetAll = HGetAll("foo")
    sers(hgetAll, "HGETALL", "foo")
    ser(hgetAll, cmd"HGETALL foo")
  }

}

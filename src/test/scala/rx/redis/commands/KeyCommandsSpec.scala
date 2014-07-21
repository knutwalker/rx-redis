package rx.redis.commands

import rx.redis.util._
import scala.concurrent.duration._

class KeyCommandsSpec extends CommandsSuite {

  test("DEL") {
    val del = Del("foo", "bar", "baz")
    cmds(del, "DEL", "foo", "bar", "baz")
    cmd(del, resp"DEL foo bar baz")
  }

  test("EXISTS") {
    val exists = Exists("foo")
    cmds(exists, "EXISTS", "foo")
    cmd(exists, resp"EXISTS foo")
  }

  test("EXPIRE") {
    val expire = Expire("foo", 42 seconds)
    cmds(expire, "EXPIRE", "foo", "42")
    cmd(expire, resp"EXPIRE foo 42")
  }

  test("EXPIREAT") {
    val in42Seconds = ((System.currentTimeMillis() + 42000) / 1000).toString
    val deadline = 42.seconds.fromNow
    val expireAt = ExpireAt("foo", deadline)
    cmds(expireAt, "EXPIREAT", "foo", in42Seconds.toString)
    cmd(expireAt, resp"EXPIREAT foo $in42Seconds")
  }

  test("KEYS") {
    val keys = Keys("f*bar?")
    cmds(keys, "KEYS", "f*bar?")
    cmd(keys, resp"KEYS f*bar?")
  }

  test("RANDOMKEY") {
    val randomKey = RandomKey
    cmds(randomKey, "RANDOMKEY")
    cmd(randomKey, resp"RANDOMKEY")
  }

  test("TTL") {
    val ttl = Ttl("foo")
    cmds(ttl, "TTL", "foo")
    cmd(ttl, resp"TTL foo")
  }

}

package rx.redis.commands

import rx.redis.util._
import scala.concurrent.duration._

class KeyCommandsSpec extends CommandsSuite {

  test("DEL") {
    val del = Del("foo", "bar", "baz")
    cmds(del, "*4", "$3", "DEL", "$3", "foo", "$3", "bar", "$3", "baz")
    cmd(del, resp"DEL foo bar baz")
  }

  test("EXISTS") {
    val exists = Exists("foo")
    cmds(exists, "*2", "$6", "EXISTS", "$3", "foo")
    cmd(exists, resp"EXISTS foo")
  }

  test("EXPIRE") {
    val expire = Expire("foo", 42 seconds)
    cmds(expire, "*3", "$6", "EXPIRE", "$3", "foo", "$2", "42")
    cmd(expire, resp"EXPIRE foo 42")
  }

  test("EXPIREAT") {
    val in42Seconds = ((System.currentTimeMillis() + 42000) / 1000).toString
    val deadline = 42.seconds.fromNow
    val expireAt = ExpireAt("foo", deadline)
    cmds(expireAt, "*3", "$8", "EXPIREAT", "$3", "foo", "$10", in42Seconds.toString)
    cmd(expireAt, resp"EXPIREAT foo $in42Seconds")
  }

  test("KEYS") {
    val keys = Keys("f*bar?")
    cmds(keys, "*2", "$4", "KEYS", "$6", "f*bar?")
    cmd(keys, resp"KEYS f*bar?")
  }

  test("RANDOMKEY") {
    val randomKey = RandomKey
    cmds(randomKey, "*1", "$9", "RANDOMKEY")
    cmd(randomKey, resp"RANDOMKEY")
  }

  test("TTL") {
    val ttl = Ttl("foo")
    cmds(ttl, "*2", "$3", "TTL", "$3", "foo")
    cmd(ttl, resp"TTL foo")
  }

}

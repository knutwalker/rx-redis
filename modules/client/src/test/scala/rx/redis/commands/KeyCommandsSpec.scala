package rx.redis.commands

import rx.redis._
import scala.concurrent.duration._

class KeyCommandsSpec extends CommandsSuite {

  test("DEL") {
    val del = Del("foo", "bar", "baz")
    sers(del, "DEL", "foo", "bar", "baz")
    ser(del, cmd"DEL foo bar baz")
  }

  test("EXISTS") {
    val exists = Exists("foo")
    sers(exists, "EXISTS", "foo")
    ser(exists, cmd"EXISTS foo")
  }

  test("EXPIRE") {
    val expire = Expire("foo", 42 seconds)
    sers(expire, "EXPIRE", "foo", "42")
    ser(expire, cmd"EXPIRE foo 42")
  }

  test("EXPIREAT") {
    val in42Seconds = ((System.currentTimeMillis() + 42000) / 1000).toString
    val deadline = 42.seconds.fromNow
    val expireAt = ExpireAt("foo", deadline)
    sers(expireAt, "EXPIREAT", "foo", in42Seconds.toString)
    ser(expireAt, cmd"EXPIREAT foo $in42Seconds")
  }

  test("KEYS") {
    val keys = Keys("f*bar?")
    sers(keys, "KEYS", "f*bar?")
    ser(keys, cmd"KEYS f*bar?")
  }

  test("RANDOMKEY") {
    val randomKey = RandomKey
    sers(randomKey, "RANDOMKEY")
    ser(randomKey, cmd"RANDOMKEY")
  }

  test("TTL") {
    val ttl = Ttl("foo")
    sers(ttl, "TTL", "foo")
    ser(ttl, cmd"TTL foo")
  }

}

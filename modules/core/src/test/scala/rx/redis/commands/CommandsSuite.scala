package rx.redis.commands

import org.scalatest.FunSuite

import rx.redis.resp.{RespArray, RespBytes, DataType}
import rx.redis.serialization.Writes

import java.nio.charset.StandardCharsets


class CommandsSuite extends FunSuite {

  val charset = StandardCharsets.UTF_8

  protected def pretty(s: String, snip: Option[Int] = None) =
    Some(s.replaceAllLiterally("\r\n", "\\r\\n")).map(s => snip.fold(s)(s.take)).get

  protected def ser[A: Writes](c: A, expected: DataType) = {
    val buf = Writes[A].write(c)
    assert(buf == expected)
  }

  protected def sers[A: Writes](c: A, expectedParts: String*) =
    ser(c, RespArray(expectedParts.map(RespBytes(_)).toArray))
}

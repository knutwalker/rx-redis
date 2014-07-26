package rx.redis.pipeline

import io.netty.buffer.UnpooledByteBufAllocator
import org.scalatest.{ FunSuite, Inside }
import rx.redis.resp._
import rx.redis.util.Utf8

class SerializerSpec extends FunSuite with Inside {

  val alloc = UnpooledByteBufAllocator.DEFAULT

  private def compare(dt: DataType, resp: String) = {
    val buf = alloc.buffer()
    Serializer(dt, buf)
    val result = buf.toString(Utf8)
    assert(result == resp)
    assert(Deserializer(buf) == dt)
    buf.release()
  }

  test("serialize strings") {
    compare(RespString("OK"), "+OK\r\n")
  }

  test("serialize errors") {
    compare(RespError("Error"), "-Error\r\n")
  }

  test("serialize integers") {
    compare(RespInteger(42), ":42\r\n")
    compare(RespInteger(Long.MaxValue), ":9223372036854775807\r\n")

    compare(RespInteger(-42), ":-42\r\n")
    compare(RespInteger(Long.MinValue), ":-9223372036854775808\r\n")
  }

  test("serialize bulk strings") {
    compare(RespBytes("foobar"), "$6\r\nfoobar\r\n")
    compare(RespBytes("foo\r\nbar"), "$8\r\nfoo\r\nbar\r\n")
  }

  test("serialize an empty string") {
    compare(RespBytes(""), "$0\r\n\r\n")
  }

  test("serialize the null string") {
    compare(NullString, "$-1\r\n")
  }

  test("serialize arrays") {
    compare(RespArray(Array(RespBytes("foo"), RespBytes("bar"))), "*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n")
  }

  test("serialize integer arrays") {
    compare(RespArray(Array(RespInteger(1), RespInteger(2), RespInteger(3))), "*3\r\n:1\r\n:2\r\n:3\r\n")
  }

  test("serialize mixed arrays") {
    compare(
      RespArray(Array(
        RespInteger(1),
        RespInteger(2),
        RespInteger(3),
        RespInteger(4),
        RespBytes("foobar")
      )),
      "*5\r\n:1\r\n:2\r\n:3\r\n:4\r\n$6\r\nfoobar\r\n"
    )
  }

  test("serialize an empty array") {
    compare(RespArray(Array()), "*0\r\n")
  }

  test("serialize the null array") {
    compare(NullArray, "*-1\r\n")
  }

  test("serialize nested arrays") {
    compare(
      RespArray(Array(
        RespArray(Array(
          RespInteger(1),
          RespInteger(2),
          RespInteger(3)
        )),
        RespArray(Array(
          RespString("Foo"),
          RespError("Bar")
        ))
      )),
      "*2\r\n*3\r\n:1\r\n:2\r\n:3\r\n*2\r\n+Foo\r\n-Bar\r\n"
    )
  }

  // sad path behavior

  test("buffer under capacity") {
    val buf = alloc.buffer(4, 4)
    val ex = intercept[IndexOutOfBoundsException] {
      Serializer(NullString, buf)
    }
    assert(ex.getMessage.contains("exceeds maxCapacity(4)"))
    buf.release()
  }

  test("disallow newlines in strings") {
    val ex = intercept[IllegalArgumentException] {
      RespString("foo\r\nbar")
    }
    assert(ex.getMessage == "requirement failed: A RESP String must not contain [\\r\\n].")
  }
}

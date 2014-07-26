package rx.redis.protocol

import io.netty.buffer.{Unpooled, PooledByteBufAllocator}

import org.scalatest.{FunSuite, Inside}

import rx.redis.resp._
import rx.redis.util.Utf8


class DeserializerSpec extends FunSuite with Inside {

  val alloc = PooledByteBufAllocator.DEFAULT

  def compare(resp: String, expecteds: DataType*): Unit = {
    Deserializer.parseAll(resp, alloc).zip(expecteds) foreach {
      case (actual, expected) =>
        assert(actual == expected)
    }
  }

  def compare(resp: String)(insidePf: PartialFunction[RespType, Unit]): Unit = {
    inside(Deserializer(resp, alloc))(insidePf)
  }

  // happy path behavior

  test("deserialize simple strings") {
    compare("+OK\r\n", RespString("OK"))
  }

  test("deserialize errors") {
    compare("-Error\r\n", RespError("Error"))
  }

  test("deserialize errors as simple strings") {
    compare(
      "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n",
      RespError("WRONGTYPE Operation against a key holding the wrong kind of value"))
  }

  test("deserialize integers") {
    compare(":0\r\n", RespInteger(0))
    compare(":9223372036854775807\r\n", RespInteger(Long.MaxValue))
  }

  test("deserialize integers with negative sign") {
    compare(":-1\r\n", RespInteger(-1))
    compare(":-9223372036854775808\r\n", RespInteger(Long.MinValue))
  }

  test("deserialize bulk strings") {
    compare("$6\r\nfoobar\r\n", RespBytes("foobar"))
  }

  test("allow new lines in bulk strings") {
    compare("$8\r\nfoo\r\nbar\r\n", RespBytes("foo\r\nbar"))
  }

  test("deserialize multiple bulk strings") {
    compare("$6\r\nfoobar\r\n$4\r\n1337\r\n", RespBytes("foobar"), RespBytes("1337"))
  }

  test("deserialize an empty string") {
    compare("$0\r\n\r\n", RespBytes(""))
  }

  test("deserialize the null string") {
    compare("$-1\r\n", NullString)
  }

  test("deserialize arrays") {
    compare("*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n", RespArray(Array(RespBytes("foo"), RespBytes("bar"))))
  }

  test("deserialize integer arrays") {
    compare("*3\r\n:1\r\n:2\r\n:3\r\n", RespArray(Array(RespInteger(1), RespInteger(2), RespInteger(3))))
  }

  test("deserialize mixed arrays") {
    compare("*5\r\n:1\r\n:2\r\n:3\r\n:4\r\n$6\r\nfoobar\r\n",
      RespArray(Array(
        RespInteger(1),
        RespInteger(2),
        RespInteger(3),
        RespInteger(4),
        RespBytes("foobar")
      )))
  }

  test("deserialize an empty array") {
    compare("*0\r\n", RespArray(Array()))
  }

  test("deserialize the null array") {
    compare("*-1\r\n", NullArray)
  }

  test("deserialize nested arrays") {
    compare("*2\r\n*3\r\n:1\r\n:2\r\n:3\r\n*2\r\n+Foo\r\n-Bar\r\n",
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
      ))
    )
  }

  // sad path behavior

  test("missing CrLf for simple strings") {
    compare("+OK") {
      case NotEnoughData =>
    }
  }

  test("length overflow in bulk strings") {
    compare("$9\r\nfoobar\r\n") {
      case NotEnoughData =>
    }
  }

  test("length underflow in bulk strings") {
    compare("$3\r\nfoobar\r\n") {
      case ProtocolError(pos, found, expected) =>
        assert(expected === List('\r'.toByte))
        assert(pos == 7)
        assert(found == 'b'.toByte)
    }
  }

  test("size overflow in arrays") {
    compare("*3\r\n:1\r\n:2\r\n") {
      case NotEnoughData =>
    }
  }

  test("size underflow in arrays") {
    compare("*1\r\n:1\r\n:2\r\n", RespArray(Array(RespInteger(1))))
  }

  test("missing type marker") {
    compare("?MISSING") {
      case ProtocolError(pos, found,expected) =>
        assert(expected == List('+'.toByte, '-'.toByte, ':'.toByte, '$'.toByte, '*'.toByte))
        assert(pos == 0)
        assert(found == '?'.toByte)
    }
  }

  test("buffer under capacity") {
    val resp = "*2\r\n*3\r\n:1\r\n$3\r\nBaz\r\n:3\r\n*2\r\n+Foo\r\n-Bar\r\n"
    val bytes = resp.getBytes(Utf8)
    val buf = Unpooled.wrappedBuffer(bytes)

    for (i <- bytes.indices) {
      val bb = buf.duplicate()
      bb.writerIndex(i)
      Deserializer.parseAll(bb) foreach { actual =>
        assert(actual == NotEnoughData)
      }
    }
  }

  /*
    according to spec:
     Integer RESP is guaranteed to be a valid 64 bit int, so not overflow testing
   */
}

package rx.redis.resp

import org.scalatest.{FunSuite, Inside}

class RespSpec extends FunSuite with Inside {

  def compare(resp: String, expecteds: DataType*): Unit = {
    val parser = Parser(resp)
    expecteds foreach { expected =>
      assert(parser() == expected)
    }
  }

  def compare(resp: String)(insidePf: PartialFunction[RespType, Unit]): Unit = {
    val parser = Parser(resp)
    inside(parser())(insidePf)
  }

  // happy path behavior

  test("parse simple strings") {
    compare("+OK\r\n", RespString("OK"))
  }

  test("parse errors") {
    compare("-Error\r\n", RespError("Error"))
  }

  test("parse errors as simple strings") {
    compare(
      "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n",
      RespError("WRONGTYPE Operation against a key holding the wrong kind of value"))
  }

  test("parse integers") {
    compare(":0\r\n", RespInteger(0))
    compare(":9223372036854775807\r\n", RespInteger(Long.MaxValue))
  }

  test("parse integers with negative sign") {
    compare(":-1\r\n", RespInteger(-1))
    compare(":-9223372036854775808\r\n", RespInteger(Long.MinValue))
  }

  test("parse bulk strings") {
    compare("$6\r\nfoobar\r\n", RespString("foobar"))
  }

  test("allow new lines in bulk strings") {
    compare("$8\r\nfoo\r\nbar\r\n", RespString("foo\r\nbar"))
  }

  test("parse multiple bulk strings") {
    compare("$6\r\nfoobar\r\n$4\r\n1337\r\n", RespString("foobar"), RespString("1337"))
  }

  test("parse an empty string") {
    compare("$0\r\n\r\n", RespString(""))
  }

  test("parse the null string") {
    compare("$-1\r\n", NullString)
  }

  test("parse arrays") {
    compare("*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n", RespArray(List(RespString("foo"), RespString("bar"))))
  }

  test("parse integer arrays") {
    compare("*3\r\n:1\r\n:2\r\n:3\r\n", RespArray(List(RespInteger(1), RespInteger(2), RespInteger(3))))
  }

  test("parse mixed arrays") {
    compare("*5\r\n:1\r\n:2\r\n:3\r\n:4\r\n$6\r\nfoobar\r\n",
      RespArray(List(
        RespInteger(1),
        RespInteger(2),
        RespInteger(3),
        RespInteger(4),
        RespString("foobar")
      )))
  }

  test("parse an empty array") {
    compare("*0\r\n", RespArray(List()))
  }

  test("parse the null array") {
    compare("*-1\r\n", NullArray)
  }

  test("parse nested arrays") {
    compare("*2\r\n*3\r\n:1\r\n:2\r\n:3\r\n*2\r\n+Foo\r\n-Bar\r\n",
      RespArray(List(
        RespArray(List(
          RespInteger(1),
          RespInteger(2),
          RespInteger(3)
        )),
        RespArray(List(
          RespString("Foo"),
          RespError("Bar")
        ))
      ))
    )
  }

  // bad path behavior

  test("missing CrLf for simple strings") {
    compare("+OK") {
      case NotEnoughData(bb) =>
        assert(bb.readableBytes() == bb.writerIndex())
        assert(bb.toString(Parser.utf8) == "+OK")
    }
  }

  test("length overflow in bulk strings") {
    compare("$9\r\nfoobar\r\n") {
      case NotEnoughData(bb) =>
        assert(bb.readableBytes() == bb.writerIndex())
        assert(bb.toString(Parser.utf8) == "$9\r\nfoobar\r\n")
    }
  }

  test("length underflow in bulk strings") {
    compare("$3\r\nfoobar\r\n") {
      case ProtocolError(bb, expected) =>
        assert(expected === List('\r'.toByte))
        assert(bb.getByte(bb.readerIndex()) == 'b'.toByte)
    }
  }

  test("size overflow in arrays") {
    compare("*3\r\n:1\r\n:2\r\n") {
      case NotEnoughData(bb) =>
        assert(bb.readableBytes() == bb.writerIndex())
        assert(bb.toString(Parser.utf8) == "*3\r\n:1\r\n:2\r\n")
    }
  }

  test("size underflow in arrays") {
    compare("*1\r\n:1\r\n:2\r\n", RespArray(List(RespInteger(1))))
  }

  test("missing type marker") {
    compare("?MISSING") {
      case ProtocolError(bb, expected) =>
        assert(expected == List('+'.toByte, '-'.toByte, ':'.toByte, '$'.toByte, '*'.toByte))
        assert(bb.getByte(bb.readerIndex()) == '?'.toByte)
    }
  }

  /*
    according to spec:
     Integer RESP is guaranteed to be a valid 64 bit int, so not overflow testing
   */
}

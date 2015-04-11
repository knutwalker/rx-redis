/*
 * Copyright 2014 – 2015 Paul Horn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.redis.serialization

import scala.annotation.tailrec

import io.netty.buffer.{ ByteBuf, Unpooled }

import rx.redis.serialization.Deserializer.{ ProtocolError, NotEnoughData }
import rx.redis.resp._
import rx.redis.util._

import org.scalatest.{ FunSuite, Inside }

class DeserializerSpec extends FunSuite with Inside {

  implicit val bytesAccess = ByteBufAccess

  @tailrec final def loop(bs: ByteBuf, d: Deserializer[ByteBuf])(f: RespType ⇒ Unit): Unit = {
    f(d(bs))
    if (bs.isReadable) {
      loop(bs, d)(f)
    }
  }

  def compare(resp: String, expecteds: RespType*): Unit = {
    val bytes = Unpooled.wrappedBuffer(resp.getBytes(Utf8))
    val d = new Deserializer[ByteBuf]
    val expectedsIterator = expecteds.iterator
    loop(bytes, d) { actual ⇒
      if (expectedsIterator.hasNext) {
        val expected = expectedsIterator.next()
        assert(actual == expected)
      }
    }
  }

  def compare(resp: String)(insidePf: PartialFunction[Throwable, Unit]): Unit = {
    val d = new Deserializer[ByteBuf]
    val bytes = Unpooled.wrappedBuffer(resp.getBytes(Utf8))
    try d(bytes) catch insidePf
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
    compare("*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n", RespArray(RespBytes("foo"), RespBytes("bar")))
  }

  test("deserialize integer arrays") {
    compare("*3\r\n:1\r\n:2\r\n:3\r\n", RespArray(RespInteger(1), RespInteger(2), RespInteger(3)))
  }

  test("deserialize mixed arrays") {
    compare("*5\r\n:1\r\n:2\r\n:3\r\n:4\r\n$6\r\nfoobar\r\n",
      RespArray(
        RespInteger(1),
        RespInteger(2),
        RespInteger(3),
        RespInteger(4),
        RespBytes("foobar")
      ))
  }

  test("deserialize an empty array") {
    compare("*0\r\n", RespArray.empty)
  }

  test("deserialize the null array") {
    compare("*-1\r\n", NullArray)
  }

  test("deserialize nested arrays") {
    compare("*2\r\n*3\r\n:1\r\n:2\r\n:3\r\n*2\r\n+Foo\r\n-Bar\r\n",
      RespArray(
        RespArray(
          RespInteger(1),
          RespInteger(2),
          RespInteger(3)
        ),
        RespArray(
          RespString("Foo"),
          RespError("Bar")
        )
      )
    )
  }

  // sad path behavior

  test("missing CrLf for simple strings") {
    compare("+OK") {
      case NotEnoughData ⇒
    }
  }

  test("length overflow in bulk strings") {
    compare("$9\r\nfoobar\r\n") {
      case NotEnoughData ⇒
    }
  }

  test("length underflow in bulk strings") {
    compare("$3\r\nfoobar\r\n") {
      case ProtocolError(pos, found, expected) ⇒
        assert(expected === List('\r'.toByte))
        assert(pos == 7)
        assert(found == 'b'.toByte)
    }
  }

  test("size overflow in arrays") {
    compare("*3\r\n:1\r\n:2\r\n") {
      case NotEnoughData ⇒
    }
  }

  test("size underflow in arrays") {
    compare("*1\r\n:1\r\n:2\r\n", RespArray(RespInteger(1)))
  }

  test("missing type marker") {
    compare("?MISSING") {
      case ProtocolError(pos, found, expected) ⇒
        assert(expected == List('+'.toByte, '-'.toByte, ':'.toByte, '$'.toByte, '*'.toByte))
        assert(pos == 0)
        assert(found == '?'.toByte)
    }
  }

  test("buffer under capacity") {
    val resp = "*2\r\n*3\r\n:1\r\n$3\r\nBaz\r\n:3\r\n*2\r\n+Foo\r\n-Bar\r\n"
    val bytesArray = resp.getBytes(Utf8)
    val bytes = Unpooled.wrappedBuffer(bytesArray)

    val d = new Deserializer[ByteBuf]

    for (i ← bytesArray.indices) {
      val bb = bytes.duplicate()
      bb.writerIndex(i)

      intercept[NotEnoughData.type] {
        loop(bb, d) { actual ⇒ fail("Should not have parsed anything") }
      }
    }
  }

  /*
    according to spec:
     Integer RESP is guaranteed to be a valid 64 bit int, so not overflow testing
   */
}

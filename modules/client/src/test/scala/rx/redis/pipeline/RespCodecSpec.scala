/*
 * Copyright 2014 Paul Horn
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

package rx.redis.pipeline

import io.netty.buffer.{ ByteBuf, Unpooled }
import io.netty.channel.embedded.EmbeddedChannel

import rx.redis.resp._
import rx.redis.util.Utf8

import org.scalatest.{ BeforeAndAfter, FunSuite }

class RespCodecSpec extends FunSuite with BeforeAndAfter {

  private def compare(in: DataType, out: String) = {
    val byteData = Unpooled.copiedBuffer(out, Utf8)
    assert(channel.writeOutbound(in))
    assert(channel.readOutbound[ByteBuf]() == byteData)

    assert(channel.writeInbound(byteData))
    assert(channel.readInbound[DataType]() == in)
  }

  private var channel: EmbeddedChannel = _

  before {
    channel = new EmbeddedChannel(new RespCodec)
  }

  after {
    channel.close().sync()
  }

  test("should handle strings") {
    compare(RespString("foo"), "+foo\r\n")
  }

  test("should handle integers") {
    compare(RespInteger(42), ":42\r\n")
  }

  test("should handle errors") {
    compare(RespError("foo"), "-foo\r\n")
  }

  test("should handle byte strings") {
    compare(RespBytes("foo\r\nbar"), "$8\r\nfoo\r\nbar\r\n")
  }

  test("should handle arrays") {
    compare(RespArray(Array(RespString("foo"), RespBytes("bar"))), "*2\r\n+foo\r\n$3\r\nbar\r\n")
  }

  test("should fail on outbound writing for non data types ") {
    val ex = intercept[IllegalArgumentException] {
      channel.writeOutbound(NotEnoughData)
    }
    assert(channel.readOutbound[ByteBuf]() == null)
    assert(ex.getMessage == "msg is not a [rx.redis.resp.DataType].")
  }

  test("should fail on inbound writing for non ByteBufs") {
    val ex = intercept[IllegalArgumentException] {
      channel.writeInbound("foo")
    }
    assert(channel.readInbound[ByteBuf]() == null)
    assert(ex.getMessage == "msg is not a [io.netty.buffer.ByteBuf].")
  }

  test("should accumulate chunked inbound messages") {
    val chunks = List("+", "An", " Ove", "rly", " Lo", "ng Res", "ponse", "\r\n") map { c ⇒
      Unpooled.copiedBuffer(c, Utf8)
    }
    assert(channel.writeInbound(chunks: _*))
    assert(channel.readInbound[DataType]() == RespString("An Overly Long Response"))
    assert(channel.readInbound[DataType]() == null)
  }

  test("should parse multiple answers") {
    val resp = "+Foo\r\n+Bar\r\n$3\r\nbaz\r\n$3\r\nqux\r\n"
    assert(channel.writeInbound(Unpooled.copiedBuffer(resp, Utf8)))
    assert(channel.readInbound[DataType]() == RespString("Foo"))
    assert(channel.readInbound[DataType]() == RespString("Bar"))
    assert(channel.readInbound[DataType]() == RespBytes("baz"))
    assert(channel.readInbound[DataType]() == RespBytes("qux"))
    assert(channel.readInbound[DataType]() == null)
  }
}

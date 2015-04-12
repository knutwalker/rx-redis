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

import rx.redis.resp._
import rx.redis.resp.Protocol._
import rx.redis.util._

import io.netty.buffer.ByteBuf

import collection.immutable.VectorBuilder
import scala.annotation.tailrec
import scala.util.control.NoStackTrace

object Deserializer {
  sealed trait RespFailure

  private[redis] final val NotEnoughData = new RuntimeException with NoStackTrace with RespFailure
  private[redis] final case class ProtocolError(pos: Int, found: Char, expected: List[Byte]) extends RuntimeException(
    s"Protocol error at char $pos, expected [${expected mkString ", "}], but found [$found]") with RespFailure

  object RespFailure {
    def apply(t: Throwable): Boolean = t match {
      case NotEnoughData | _: ProtocolError ⇒ true
      case _                                ⇒ false
    }
    def unapply(t: Throwable): Option[Throwable] = if (apply(t)) Some(t) else None
  }
}
final class Deserializer {

  private[this] def notEnoughData(bytes: ByteBuf) =
    throw Deserializer.NotEnoughData

  private[this] def unknownType(bytes: ByteBuf) =
    expected(bytes, typeChars: _*)

  private[this] def expected(bytes: ByteBuf, expected: Byte*) = {
    val pos = bytes.readerIndex()
    val found = bytes.getByte(pos).toChar
    throw Deserializer.ProtocolError(pos, found, expected.toList)
  }

  private[this] def peek(bytes: ByteBuf) =
    bytes.getByte(bytes.readerIndex())

  private[this] def read(bytes: ByteBuf) =
    bytes.readByte()

  private[this] def skip(bytes: ByteBuf) =
    bytes.skipBytes(1)

  private[this] def requireLen(bytes: ByteBuf, len: Int) =
    bytes.isReadable(len)

  private[this] def read(bytes: ByteBuf, b: Byte): Unit = {
    if (!bytes.isReadable) notEnoughData(bytes)
    else if (peek(bytes) != b) expected(bytes, b)
    else skip(bytes)
  }

  private[this] def andRequireCrLf(bytes: ByteBuf, value: RespType) = {
    read(bytes, Cr)
    read(bytes, Lf)
    value
  }

  private[this] def parseLen(bytes: ByteBuf) =
    parseNumInt(bytes)

  private[this] def parseInteger(bytes: ByteBuf) =
    RespInteger(parseNumLong(bytes))

  @tailrec
  private[this] def parseNumInt(bytes: ByteBuf, n: Int, neg: Int): Int = {
    if (!bytes.isReadable) {
      notEnoughData(bytes)
    } else {
      val current = read(bytes)
      current match {
        case Cr ⇒
          read(bytes, Lf)
          n * neg
        case Minus ⇒ parseNumInt(bytes, n, -1)
        case _     ⇒ parseNumInt(bytes, (n * 10) + (current - '0'), neg)
      }
    }
  }

  @tailrec
  private[this] def parseNumLong(bytes: ByteBuf, n: Long, neg: Long): Long = {
    if (!bytes.isReadable) {
      notEnoughData(bytes)
    } else {
      val current = read(bytes)
      current match {
        case Cr ⇒
          read(bytes, Lf)
          n * neg
        case Minus ⇒ parseNumLong(bytes, n, -1)
        case _     ⇒ parseNumLong(bytes, (n * 10) + (current - '0'), neg)
      }
    }
  }

  private[this] def parseNumInt(bytes: ByteBuf): Int =
    parseNumInt(bytes, 0, 1)

  private[this] def parseNumLong(bytes: ByteBuf): Long =
    parseNumLong(bytes, 0L, 1L)

  private[this] def readStringOfLen(bytes: ByteBuf, len: Int)(ct: ByteBuf ⇒ RespType) = {
    if (!requireLen(bytes, len)) notEnoughData(bytes)
    else andRequireCrLf(bytes, ct(bytes.readBytes(len)))
  }

  private[this] def parseSimpleString(bytes: ByteBuf) = {
    val len = bytes.bytesBefore(Cr)
    if (len == -1) notEnoughData(bytes)
    else readStringOfLen(bytes, len)(b ⇒ RespString(b.toString(Utf8)))
  }

  private[this] def parseError(bytes: ByteBuf) = {
    val len = bytes.bytesBefore(Cr)
    if (len == -1) notEnoughData(bytes)
    else readStringOfLen(bytes, len)(b ⇒ RespError(b.toString(Utf8)))
  }

  private[this] def parseBulkString(bytes: ByteBuf) = {
    val len = parseLen(bytes)
    if (len == -1) NullString
    // TODO: Do we need to retain or can we ignore this? Maybe use b directly without slice
    else readStringOfLen(bytes, len)(b ⇒ RespBytes.wrap(b.slice().retain()))
  }

  private[this] def parseArray(bytes: ByteBuf) = {
    val size = parseLen(bytes)
    if (size == -1) NullArray
    else {
      val lb = new VectorBuilder[RespType]()
      lb.sizeHint(size)
      @tailrec def loop(n: Int): RespType = {
        if (n == 0) RespArray(lb.result())
        else {
          lb += quickApply(bytes)
          loop(n - 1)
        }
      }
      loop(size)
    }
  }

  private[this] def quickApply(bytes: ByteBuf): RespType = {
    if (!bytes.isReadable) notEnoughData(bytes)
    else {
      val firstByte = peek(bytes)
      firstByte match {
        case Plus ⇒
          bytes.skipBytes(1)
          parseSimpleString(bytes)
        case Minus ⇒
          bytes.skipBytes(1)
          parseError(bytes)
        case Colon ⇒
          bytes.skipBytes(1)
          parseInteger(bytes)
        case Dollar ⇒
          bytes.skipBytes(1)
          parseBulkString(bytes)
        case Asterisk ⇒
          bytes.skipBytes(1)
          parseArray(bytes)
        case _ ⇒
          unknownType(bytes)
      }
    }
  }

  def apply(bytes: ByteBuf): RespType = {
    bytes.markReaderIndex()
    try {
      quickApply(bytes)
    } catch {
      case Deserializer.RespFailure(ex) ⇒
        bytes.resetReaderIndex()
        throw ex
    }
  }
}

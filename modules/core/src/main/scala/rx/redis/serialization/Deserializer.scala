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
import scala.collection.mutable.ArrayBuffer

import rx.redis.resp._
import rx.redis.util._

import scala.util.control.NoStackTrace

object Deserializer {
  private[redis] final val NotEnoughData = new RuntimeException with NoStackTrace
  private[redis] final case class ProtocolError(pos: Int, found: Char, expected: List[Byte])
    extends RuntimeException(s"Protocol error at char $pos, expected [${expected mkString ", "}], but found [$found]")

  object RespFailure {
    def apply(t: Throwable): Boolean = t match {
      case NotEnoughData | _: ProtocolError ⇒ true
      case _                                ⇒ false
    }
    def unapply(t: Throwable): Option[Throwable] = if (apply(t)) Some(t) else None
  }
}
final class Deserializer[A](implicit A: BytesAccess[A]) {
  import rx.redis.resp.Protocol._

  private[this] def notEnoughData(bytes: A) =
    throw Deserializer.NotEnoughData

  private[this] def unknownType(bytes: A) =
    expected(bytes, typeChars: _*)

  private[this] def expected(bytes: A, expected: Byte*) = {
    val pos = A.readerIndex(bytes)
    val found = A.getByteAt(bytes, pos).toChar
    throw Deserializer.ProtocolError(pos, found, expected.toList)
  }

  private[this] def peek(bytes: A) =
    A.getByteAt(bytes, A.readerIndex(bytes))

  private[this] def read(bytes: A) =
    A.readNextByte(bytes)

  private[this] def skip(bytes: A) =
    A.skipBytes(bytes, 1)

  private[this] def requireLen(bytes: A, len: Int) =
    A.isReadable(bytes, len)

  private[this] def read(bytes: A, b: Byte): Unit = {
    if (!A.isReadable(bytes)) notEnoughData(bytes)
    else if (peek(bytes) != b) expected(bytes, b)
    else skip(bytes)
  }

  private[this] def andRequireCrLf(bytes: A, value: RespType) = {
    read(bytes, Cr)
    read(bytes, Lf)
    value
  }

  private[this] def parseLen(bytes: A) =
    parseNumInt(bytes)

  private[this] def parseInteger(bytes: A) =
    RespInteger(parseNumLong(bytes))

  @tailrec
  private[this] def parseNumInt(bytes: A, n: Int, neg: Int): Int = {
    if (!A.isReadable(bytes)) {
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
  private[this] def parseNumLong(bytes: A, n: Long, neg: Long): Long = {
    if (!A.isReadable(bytes)) {
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

  private[this] def parseNumInt(bytes: A): Int =
    parseNumInt(bytes, 0, 1)

  private[this] def parseNumLong(bytes: A): Long =
    parseNumLong(bytes, 0L, 1L)

  private[this] def readStringOfLen(bytes: A, len: Int)(ct: A ⇒ RespType) = {
    if (!requireLen(bytes, len)) notEnoughData(bytes)
    else andRequireCrLf(bytes, ct(A.readBytes(bytes, len)))
  }

  private[this] def parseSimpleString(bytes: A) = {
    val len = A.bytesBefore(bytes, Cr)
    if (len == -1) notEnoughData(bytes)
    else readStringOfLen(bytes, len)(b ⇒ RespString(A.toString(b, Utf8)))
  }

  private[this] def parseError(bytes: A) = {
    val len = A.bytesBefore(bytes, Cr)
    if (len == -1) notEnoughData(bytes)
    else readStringOfLen(bytes, len)(b ⇒ RespError(A.toString(b, Utf8)))
  }

  private[this] def parseBulkString(bytes: A) = {
    val len = parseLen(bytes)
    if (len == -1) NullString
    else readStringOfLen(bytes, len)(b ⇒ RespBytes(A.toByteArray(b)))
  }

  private[this] def parseArray(bytes: A) = {
    val size = parseLen(bytes)
    if (size == -1) NullArray
    else {
      val lb = new ArrayBuffer[RespType](size)
      @tailrec def loop(n: Int): RespType = {
        if (n == 0) RespArray(lb.toArray)
        else {
          lb += quickApply(bytes)
          loop(n - 1)
        }
      }
      loop(size)
    }
  }

  private[this] def quickApply(bytes: A): RespType = {
    if (!A.isReadable(bytes)) notEnoughData(bytes)
    else {
      val firstByte = peek(bytes)
      firstByte match {
        case Plus ⇒
          A.skipBytes(bytes, 1)
          parseSimpleString(bytes)
        case Minus ⇒
          A.skipBytes(bytes, 1)
          parseError(bytes)
        case Colon ⇒
          A.skipBytes(bytes, 1)
          parseInteger(bytes)
        case Dollar ⇒
          A.skipBytes(bytes, 1)
          parseBulkString(bytes)
        case Asterisk ⇒
          A.skipBytes(bytes, 1)
          parseArray(bytes)
        case _ ⇒
          unknownType(bytes)
      }
    }
  }

  def apply(bytes: A): RespType = {
    A.markReaderIndex(bytes)
    try {
      quickApply(bytes)
    } catch {
      case Deserializer.RespFailure(ex) ⇒
        A.resetReaderIndex(bytes)
        throw ex
    }
  }
}

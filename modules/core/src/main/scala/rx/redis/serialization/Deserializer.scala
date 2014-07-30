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

package rx.redis.serialization

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

import rx.redis.resp._
import rx.redis.util._

final class Deserializer[A](implicit A: BytesAccess[A]) {
  import rx.redis.resp.Protocol._

  private def notEnoughData(bytes: A) = {
    A.resetReaderIndex(bytes)
    NotEnoughData
  }

  private def unknownType(bytes: A) =
    expected(bytes, typeChars: _*)

  private def expected(bytes: A, expected: Byte*) = {
    val pos = A.readerIndex(bytes)
    val found = A.getByteAt(bytes, pos).toChar
    A.resetReaderIndex(bytes)
    ProtocolError(pos, found, expected.toList)
  }

  private def peek(bytes: A) =
    A.getByteAt(bytes, A.readerIndex(bytes))

  private def read(bytes: A) =
    A.readNextByte(bytes)

  private def skip(bytes: A) =
    A.skipBytes(bytes, 1)

  private def requireLen(bytes: A, len: Int) =
    A.isReadable(bytes, len)

  private def read(bytes: A, b: Byte): Option[ErrorType] = {
    if (!A.isReadable(bytes)) Some(notEnoughData(bytes))
    else if (peek(bytes) != b) Some(expected(bytes, b))
    else {
      skip(bytes)
      None
    }
  }

  private def andRequireCrLf(bytes: A, value: RespType) =
    read(bytes, Cr).orElse(read(bytes, Lf)).getOrElse(value)

  private def parseLen(bytes: A) =
    parseNumInt(bytes)

  private def parseInteger(bytes: A) = parseNumLong(bytes) match {
    case Left(e)  ⇒ e
    case Right(l) ⇒ RespInteger(l)
  }

  @tailrec
  private def parseNumInt(bytes: A, n: Int, neg: Int): Either[ErrorType, Int] = {
    if (!A.isReadable(bytes)) {
      Left(notEnoughData(bytes))
    } else {
      val current = read(bytes)
      current match {
        case Cr    ⇒ read(bytes, Lf).toLeft(n * neg)
        case Minus ⇒ parseNumInt(bytes, n, -1)
        case b     ⇒ parseNumInt(bytes, (n * 10) + (b - '0'), neg)
      }
    }
  }

  @tailrec
  private def parseNumLong(bytes: A, n: Long, neg: Long): Either[ErrorType, Long] = {
    if (!A.isReadable(bytes)) {
      Left(notEnoughData(bytes))
    } else {
      val current = read(bytes)
      current match {
        case Cr    ⇒ read(bytes, Lf).toLeft(n * neg)
        case Minus ⇒ parseNumLong(bytes, n, -1)
        case b     ⇒ parseNumLong(bytes, (n * 10) + (b - '0'), neg)
      }
    }
  }

  private def parseNumInt(bytes: A): Either[ErrorType, Int] =
    parseNumInt(bytes, 0, 1)

  private def parseNumLong(bytes: A): Either[ErrorType, Long] =
    parseNumLong(bytes, 0L, 1L)

  private def readStringOfLen(bytes: A, len: Int)(ct: A ⇒ DataType) = {
    if (!requireLen(bytes, len)) notEnoughData(bytes)
    else andRequireCrLf(bytes, ct(A.readBytes(bytes, len)))
  }

  private def parseSimpleString(bytes: A) = {
    val len = A.bytesBefore(bytes, Cr)
    if (len == -1) notEnoughData(bytes)
    else readStringOfLen(bytes, len)(b ⇒ RespString(A.toString(b, Utf8)))
  }

  private def parseError(bytes: A) = {
    val len = A.bytesBefore(bytes, Cr)
    if (len == -1) notEnoughData(bytes)
    else readStringOfLen(bytes, len)(b ⇒ RespError(A.toString(b, Utf8)))
  }

  private def parseBulkString(bytes: A) = parseLen(bytes) match {
    case Left(ned) ⇒ ned
    case Right(len) ⇒ {
      if (len == -1) NullString
      else readStringOfLen(bytes, len)(b ⇒ RespBytes(A.toByteArray(b)))
    }
  }

  private def parseArray(bytes: A) = parseLen(bytes) match {
    case Left(ned) ⇒ ned
    case Right(size) ⇒ {
      if (size == -1) NullArray
      else {
        val lb = new ArrayBuffer[DataType](size)
        @tailrec def loop(n: Int): RespType = {
          if (n == 0) RespArray(lb.toArray)
          else quickApply(bytes) match {
            case dt: DataType ⇒
              lb += dt
              loop(n - 1)
            case et: ErrorType ⇒ et
          }
        }
        loop(size)
      }
    }
  }

  private def quickApply(bytes: A): RespType = {
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
    quickApply(bytes)
  }
}

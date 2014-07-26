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

import io.netty.buffer.{ ByteBuf, ByteBufAllocator }

import rx.redis.resp._
import rx.redis.util.Utf8

import java.nio.charset.Charset
import scala.annotation.tailrec
import scala.collection.immutable
import scala.collection.mutable.{ ArrayBuffer, ListBuffer }

object Deserializer {
  private final val INSTANCE = new Deserializer()

  private trait Num[@specialized(Int, Long) A] {
    def times(a: A, b: A): A
    def decShiftLeft(a: A, ones: Int): A
    def zero: A
    def one: A
    def minusOne: A
  }

  implicit private object IntIsNum extends Num[Int] {
    def times(a: Int, b: Int): Int = a * b
    def decShiftLeft(a: Int, ones: Int): Int = (a * 10) + ones
    val one: Int = 1
    val minusOne: Int = -1
    val zero: Int = 0
  }

  implicit private object LongIsNum extends Num[Long] {
    def times(a: Long, b: Long): Long = a * b
    def decShiftLeft(a: Long, ones: Int): Long = (a * 10) + ones
    val one: Long = 1
    val minusOne: Long = -1
    val zero: Long = 0
  }

  private def releaseAfterUse[A](bb: ByteBuf)(f: ⇒ A): A =
    try f finally bb.release()

  def apply(bb: ByteBuf): RespType = INSTANCE(bb)

  def apply(bytes: Array[Byte], alloc: ByteBufAllocator): RespType = {
    val bb = alloc.buffer(bytes.length, bytes.length)
    bb.writeBytes(bytes)
    INSTANCE(bb)
  }
  def apply(string: String, charset: Charset, alloc: ByteBufAllocator): RespType = {
    apply(string.getBytes(charset), alloc)
  }
  def apply(string: String, alloc: ByteBufAllocator): RespType = {
    apply(string, Utf8, alloc)
  }

  def foreach(bb: ByteBuf)(f: RespType ⇒ Unit): Option[NotEnoughData.type] = {
    @tailrec def loop(): Option[NotEnoughData.type] =
      if (!bb.isReadable) None
      else INSTANCE(bb) match {
        case NotEnoughData ⇒ Some(NotEnoughData)
        case e: ErrorType ⇒ {
          f(e)
          None
        }
        case x ⇒ {
          f(x)
          loop()
        }
      }
    loop()
  }
  def foreach(bytes: Array[Byte], alloc: ByteBufAllocator)(f: RespType ⇒ Unit): Option[NotEnoughData.type] = {
    val bb = alloc.buffer(bytes.length, bytes.length)
    bb.writeBytes(bytes)
    releaseAfterUse(bb)(foreach(bb)(f))
  }
  def foreach(string: String, charset: Charset, alloc: ByteBufAllocator)(f: RespType ⇒ Unit): Option[NotEnoughData.type] = {
    foreach(string.getBytes(charset), alloc)(f)
  }
  def foreach(string: String, alloc: ByteBufAllocator)(f: RespType ⇒ Unit): Option[NotEnoughData.type] = {
    foreach(string, Utf8, alloc)(f)
  }

  def parseAll(bb: ByteBuf): immutable.Seq[RespType] = {
    val lb = new ListBuffer[RespType]()
    foreach(bb)(lb += _) foreach (lb += _)
    lb.result()
  }
  def parseAll(bytes: Array[Byte], alloc: ByteBufAllocator): immutable.Seq[RespType] = {
    val bb = alloc.buffer(bytes.length, bytes.length)
    bb.writeBytes(bytes)
    releaseAfterUse(bb)(parseAll(bb))
  }
  def parseAll(string: String, charset: Charset, alloc: ByteBufAllocator): immutable.Seq[RespType] = {
    parseAll(string.getBytes(charset), alloc)
  }
  def parseAll(string: String, alloc: ByteBufAllocator): immutable.Seq[RespType] = {
    parseAll(string, Utf8, alloc)
  }
}

final class Deserializer private () {
  import rx.redis.resp.Protocol._
  import rx.redis.pipeline.Deserializer._

  private def notEnoughData(bb: ByteBuf) = {
    bb.resetReaderIndex()
    NotEnoughData
  }

  private def unknownType(bb: ByteBuf) =
    expected(bb, typeChars: _*)

  private def expected(bb: ByteBuf, expected: Byte*) = {
    val pos = bb.readerIndex()
    val found = bb.getByte(pos).toChar
    bb.resetReaderIndex()
    ProtocolError(pos, found, expected.toList)
  }

  private def peek(bb: ByteBuf) =
    bb.getByte(bb.readerIndex())

  private def read(bb: ByteBuf) =
    bb.readByte()

  private def skip(bb: ByteBuf) =
    bb.readerIndex(bb.readerIndex() + 1)

  private def requireLen(bb: ByteBuf, len: Int) =
    bb.isReadable(len)

  private def read(bb: ByteBuf, b: Byte): Option[ErrorType] = {
    if (!bb.isReadable) Some(notEnoughData(bb))
    else if (peek(bb) != b) Some(expected(bb, b))
    else {
      skip(bb)
      None
    }
  }

  private def andRequireCrLf(bb: ByteBuf, value: RespType) =
    read(bb, Cr).orElse(read(bb, Lf)).getOrElse(value)

  private def parseLen(bb: ByteBuf) =
    parseNum[Int](bb)

  private def parseInteger(bb: ByteBuf) = parseNum[Long](bb) match {
    case Left(e)  ⇒ e
    case Right(l) ⇒ RespInteger(l)
  }

  @tailrec
  private def parseNum[@specialized(Int, Long) A](bb: ByteBuf, n: A, neg: A)(implicit A: Num[A]): Either[ErrorType, A] = {
    if (!bb.isReadable) {
      Left(notEnoughData(bb))
    } else {
      val current = read(bb)
      current match {
        case Cr    ⇒ read(bb, Lf).toLeft(A.times(n, neg))
        case Minus ⇒ parseNum(bb, n, A.minusOne)
        case b     ⇒ parseNum(bb, A.decShiftLeft(n, b - '0'), neg)
      }
    }
  }
  private def parseNum[@specialized(Int, Long) A](bb: ByteBuf)(implicit A: Num[A]): Either[ErrorType, A] =
    parseNum(bb, A.zero, A.one)

  private def readStringOfLen(bb: ByteBuf, len: Int)(ct: ByteBuf ⇒ DataType) = {
    if (!requireLen(bb, len)) notEnoughData(bb)
    else andRequireCrLf(bb, ct(bb.readBytes(len)))
  }

  private def parseSimpleString(bb: ByteBuf) = {
    val len = bb.bytesBefore(Cr)
    if (len == -1) notEnoughData(bb)
    else readStringOfLen(bb, len)(b ⇒ RespString(b.toString(Utf8)))
  }

  private def parseError(bb: ByteBuf) = {
    val len = bb.bytesBefore(Cr)
    if (len == -1) notEnoughData(bb)
    else readStringOfLen(bb, len)(b ⇒ RespError(b.toString(Utf8)))
  }

  private def parseBulkString(bb: ByteBuf) = parseLen(bb) match {
    case Left(ned) ⇒ ned
    case Right(len) ⇒ {
      if (len == -1) NullString
      else readStringOfLen(bb, len)(b ⇒ {
        if (b.hasArray)
          RespBytes(b.array())
        else {
          val l = b.readableBytes()
          val a = new Array[Byte](l)
          b.readBytes(a)
          RespBytes(a)
        }
      })
    }
  }

  private def parseArray(bb: ByteBuf) = parseLen(bb) match {
    case Left(ned) ⇒ ned
    case Right(size) ⇒ {
      if (size == -1) NullArray
      else {
        val lb = new ArrayBuffer[DataType](size)
        @tailrec def loop(n: Int): RespType = {
          if (n == 0) RespArray(lb.toArray)
          else quickApply(bb) match {
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

  private def quickApply(bb: ByteBuf): RespType = {
    if (!bb.isReadable) notEnoughData(bb)
    else {
      val firstByte = peek(bb)
      firstByte match {
        case Plus ⇒
          bb.skipBytes(1)
          parseSimpleString(bb)
        case Minus ⇒
          bb.skipBytes(1)
          parseError(bb)
        case Colon ⇒
          bb.skipBytes(1)
          parseInteger(bb)
        case Dollar ⇒
          bb.skipBytes(1)
          parseBulkString(bb)
        case Asterisk ⇒
          bb.skipBytes(1)
          parseArray(bb)
        case _ ⇒
          unknownType(bb)
      }
    }
  }

  def apply(bb: ByteBuf): RespType = {
    bb.markReaderIndex()
    quickApply(bb)
  }
}

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

import rx.redis.util._

import io.netty.buffer.ByteBuf

import annotation.implicitNotFound
import util.control.NonFatal
import util.{ Failure, Success, Try }

@implicitNotFound("Cannot find a ByteBufReader of ${A}. You have to implement an rx.redis.serialization.ByteBufReader[${A}] in order to read an ${A} as a custom value.")
trait ByteBufReader[@specialized(Boolean, Byte, Int, Long) A] {

  // TODO: DecodeResult from luschy
  def fromByteBuf(bb: ByteBuf): Either[List[Throwable], A]

  final def readAndRelease(bb: ByteBuf): Either[List[Throwable], A] =
    try fromByteBuf(bb) finally bb.release()

  def map[B](f: A ⇒ B): ByteBufReader[B] = {
    val self = fromByteBuf _
    ByteBufReader(self andThen (_.right.map(f)))
  }

  def flatMap[B](f: A ⇒ Either[List[Throwable], B]): ByteBufReader[B] = {
    val self = fromByteBuf _
    ByteBufReader(self andThen (_.right.flatMap(f)))
  }

  def andRead[B](f: ⇒ ByteBufReader[B]): ByteBufReader[(A, B)] = {
    val self = fromByteBuf _
    ByteBufReader(bb ⇒ for {
      a ← self(bb).right
      b ← f.fromByteBuf(bb).right
    } yield (a, b))
  }

  def orRead[B](f: ⇒ ByteBufReader[B]): ByteBufReader[Either[A, B]] = {
    val self = fromByteBuf _
    ByteBufReader(bb ⇒ self(bb) match {
      case Left(xs) ⇒ f.fromByteBuf(bb) match {
        case Left(ys) ⇒ Left(xs ++ ys)
        case Right(b) ⇒ Right(Right(b))
      }
      case Right(a) ⇒ Right(Left(a))
    })
  }
}

object ByteBufReader {

  def read[A](f: ByteBuf ⇒ A): ByteBufReader[A] =
    apply(bb ⇒ Right(f(bb)))

  def maybeRead[A](f: ByteBuf ⇒ Option[A]): ByteBufReader[A] =
    apply(bb ⇒ f(bb).toRight(List(new IllegalArgumentException("Could not decode."))))

  def tryRead[A](f: ByteBuf ⇒ Try[A]): ByteBufReader[A] =
    apply(bb ⇒ f(bb) match {
      case Success(a)  ⇒ Right(a)
      case Failure(ex) ⇒ Left(List(ex))
    })

  def readOr[A](f: ByteBuf ⇒ Either[String, A]): ByteBufReader[A] =
    apply(bb ⇒ f(bb).left.map(m ⇒ List(new IllegalArgumentException(m))))

  def apply[A](f: ByteBuf ⇒ Either[List[Throwable], A]): ByteBufReader[A] = new ByteBufReader[A] {
    def fromByteBuf(bb: ByteBuf): Either[List[Throwable], A] = try {
      f(bb)
    } catch {
      case NonFatal(ex) ⇒ Left(List(ex))
    }
  }

  def of[A](implicit A: ByteBufReader[A]): ByteBufReader[A] = A

  implicit val readBoolean: ByteBufReader[Boolean] =
    read(_.readBoolean())

  implicit val readByte: ByteBufReader[Byte] =
    read(_.readByte())

  implicit val readChar: ByteBufReader[Char] =
    read(_.readChar())

  implicit val readInt: ByteBufReader[Int] =
    read(_.readInt())

  //  implicit val readLong: ByteBufReader[Long] =
  //    read(_.readLong())

  implicit val readDouble: ByteBufReader[Double] =
    read(_.readDouble())

  implicit val readFloat: ByteBufReader[Float] =
    read(_.readFloat())

  //  implicit val readByteArray: ByteBufReader[Array[Byte]] =
  //    readOr(bb ⇒ {
  //      val length = bb.readInt()
  //      if (!bb.isReadable(length)) Left(s"Cannot read $length more bytes")
  //      else {
  //        val target = new Array[Byte](length)
  //        bb.readBytes(target)
  //        Right(target)
  //      }
  //    })

  //  implicit val readString: ByteBufReader[String] =
  //    readByteArray.map(bs ⇒ new String(bs, Utf8))

  // Option
  // List / etc...

  //  object Unbounded {

  implicit val readUnboundedByteArray: ByteBufReader[Array[Byte]] =
    read(bb ⇒ if (bb.hasArray) {
      val backing = bb.array()
      val offset = bb.arrayOffset() + bb.readerIndex()
      val length = bb.readableBytes()
      if (offset == 0 && length == backing.length) backing
      else {
        val array = new Array[Byte](length)
        System.arraycopy(backing, offset, array, 0, length)
        array
      }
    } else {
      val length = bb.readableBytes()
      val array = new Array[Byte](length)
      bb.readBytes(array)
      array
    })

  implicit val readUnboundedString: ByteBufReader[String] =
    readUnboundedByteArray.map(bs ⇒ new String(bs, Utf8))

  implicit val readLongAsString: ByteBufReader[Long] =
    readUnboundedString.map(_.toLong)
  //  }
}

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

  def fromByteBuf(bb: ByteBuf): A

  final def readAndRelease(bb: ByteBuf): A =
    try fromByteBuf(bb) finally bb.release()

  def map[B](f: A ⇒ B): ByteBufReader[B] =
    ByteBufReader(fromByteBuf _ andThen f)

  def flatMap[B](f: A ⇒ ByteBufReader[B]): ByteBufReader[B] =
    ByteBufReader(bb ⇒ f(fromByteBuf(bb)).fromByteBuf(bb))

  def andRead[B](f: ⇒ ByteBufReader[B]): ByteBufReader[(A, B)] =
    ByteBufReader(bb ⇒ (fromByteBuf(bb), f.fromByteBuf(bb)))

  def orRead[B](f: ⇒ ByteBufReader[B]): ByteBufReader[Either[A, B]] =
    ByteBufReader(bb ⇒ try Left(fromByteBuf(bb)) catch { case NonFatal(_) ⇒ Right(f.fromByteBuf(bb)) })
}

object ByteBufReader {

  def apply[A](f: ByteBuf ⇒ A): ByteBufReader[A] = new ByteBufReader[A] {
    def fromByteBuf(bb: ByteBuf): A = // try {
      f(bb)
    //    } catch {
    //      case NonFatal(ex) ⇒ Left(List(ex))
    //    }
  }

  def of[A](implicit A: ByteBufReader[A]): ByteBufReader[A] = A

  implicit val readBoolean: ByteBufReader[Boolean] =
    apply(_.readBoolean())

  implicit val readByte: ByteBufReader[Byte] =
    apply(_.readByte())

  implicit val readChar: ByteBufReader[Char] =
    apply(_.readChar())

  implicit val readInt: ByteBufReader[Int] =
    apply(_.readInt())

  //  implicit val readLong: ByteBufReader[Long] =
  //    apply(_.readLong())

  implicit val readDouble: ByteBufReader[Double] =
    apply(_.readDouble())

  implicit val readFloat: ByteBufReader[Float] =
    apply(_.readFloat())

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
    apply(bb ⇒ if (bb.hasArray) {
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

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

import io.netty.buffer.{ Unpooled, ByteBuf }

import annotation.implicitNotFound
import concurrent.duration.{ Deadline, FiniteDuration }
import util.control.NonFatal

@implicitNotFound("Cannot find a ByteBufWriter of ${A}. You have to implement an rx.redis.serialization.ByteBufWriter[${A}] in order to write an ${A} as a custom value.")
trait ByteBufWriter[@specialized(Boolean, Byte, Int, Long) A] {

  // TODO: ByteBufOutputStream?
  def toByteBuf(bb: ByteBuf, value: A): ByteBuf

  def knownSize(value: A): Int = 0

  def hasKnownSize: Boolean = false

  final def asByteBuf(value: A): ByteBuf =
    toByteBuf(Unpooled.buffer(), value)

  def contramap[B](f: B ⇒ A): ByteBufWriter[B] = {
    val self = toByteBuf _
    val sz = knownSize _
    ByteBufWriter.applySized((bb, b) ⇒ self(bb, f(b)), sz compose f)
  }

  def contraflatMap[B](f: B ⇒ TraversableOnce[A]): ByteBufWriter[B] = {
    val self = toByteBuf _
    val sz = knownSize _
    val szz = (b: B) ⇒ f(b).foldLeft(0)((s, a) ⇒ sz(a) + s)
    ByteBufWriter.applySized((bb, b) ⇒ f(b).foldLeft(bb)(self), szz)
  }

  def andWrite[B](implicit B: ByteBufWriter[B]): ByteBufWriter[(A, B)] = {
    val self = toByteBuf _
    val sz = knownSize _
    ByteBufWriter.applySized((bb, x) ⇒ B.toByteBuf(self(bb, x._1), x._2), (ab) ⇒ sz(ab._1) + B.knownSize(ab._2))
  }

  def orWrite[B](f: ⇒ ByteBufWriter[B]): ByteBufWriter[Either[A, B]] = {
    val self = toByteBuf _
    val sz = knownSize _
    ByteBufWriter.applySized((bb, x) ⇒ x match {
      case Left(a)  ⇒ self(bb, a)
      case Right(b) ⇒ f.toByteBuf(bb, b)
    }, {
      case Left(a)  ⇒ sz(a)
      case Right(b) ⇒ f.knownSize(b)
    })
  }
}

object ByteBufWriter {

  def write[A](f: A ⇒ ByteBuf): ByteBufWriter[A] =
    apply((bb, a) ⇒ bb.writeBytes(f(a)))

  def writes[A](f: ByteBuf ⇒ A ⇒ ByteBuf): ByteBufWriter[A] =
    apply((bb, a) ⇒ f(bb)(a))

  def apply[A](f: (ByteBuf, A) ⇒ ByteBuf): ByteBufWriter[A] = new ByteBufWriter[A] {
    def toByteBuf(bb: ByteBuf, a: A): ByteBuf = try {
      f(bb, a)
    } catch {
      case NonFatal(ex) ⇒
        // ???  EncodeResult, maybe?
        bb
    }
  }

  def applySize[A](f: (ByteBuf, A) ⇒ ByteBuf, size: Int): ByteBufWriter[A] = new ByteBufWriter[A] {
    def toByteBuf(bb: ByteBuf, a: A): ByteBuf = try {
      f(bb, a)
    } catch {
      case NonFatal(ex) ⇒
        // ???  EncodeResult, maybe?
        bb
    }
    override def knownSize(value: A): Int = size
    override def hasKnownSize: Boolean = true
  }

  def applySized[A](f: (ByteBuf, A) ⇒ ByteBuf, size: A ⇒ Int): ByteBufWriter[A] = new ByteBufWriter[A] {
    def toByteBuf(bb: ByteBuf, a: A): ByteBuf = try {
      f(bb, a)
    } catch {
      case NonFatal(ex) ⇒
        // ???  EncodeResult, maybe?
        bb
    }
    override def knownSize(value: A): Int = size(value)
    override def hasKnownSize: Boolean = true
  }

  def of[A](implicit A: ByteBufWriter[A]): ByteBufWriter[A] = A

  implicit val writeBoolean: ByteBufWriter[Boolean] =
    applySize(_.writeBoolean(_), 1)

  implicit val writeByte: ByteBufWriter[Byte] =
    applySize((bb, b) ⇒ bb.writeByte(b.toInt), 1)

  implicit val writeChar: ByteBufWriter[Char] =
    applySize((bb, c) ⇒ bb.writeChar(c.toInt), 2)

  implicit val writeInt: ByteBufWriter[Int] =
    applySize(_.writeInt(_), 4)

  //  implicit val writeLong: ByteBufWriter[Long] =
  //    apply(_.writeLong(_))

  implicit val writeDouble: ByteBufWriter[Double] =
    applySize(_.writeDouble(_), 8)

  implicit val writeFloat: ByteBufWriter[Float] =
    applySize(_.writeFloat(_), 4)

  //  implicit val writeByteArray: ByteBufWriter[Array[Byte]] =
  //    (writeInt andWrite Unbounded.writeUnboundedByteArray)
  //      .contramap(bs ⇒ (bs.length, bs))

  //  implicit val writeString: ByteBufWriter[String] =
  //    writeByteArray.contramap(_.getBytes(Utf8))

  //  val writeStringAscii: ByteBufWriter[String] =
  //    (writeInt andWrite writeByte.contraflatMap[String](_.map(_.toByte)))
  //      .contramap(s ⇒ (s.length, s))

  // Option
  // List / Container / Sizable

  //  object Unbounded {

  implicit lazy val writeUnboundedByteArray: ByteBufWriter[Array[Byte]] =
    applySized(_.writeBytes(_), _.length)

  implicit val writeUnboundedString: ByteBufWriter[String] =
    writeUnboundedByteArray.contramap(_.getBytes(Utf8))

  implicit val writeLongAsString: ByteBufWriter[Long] =
    writeUnboundedString.contramap(_.toString)
  //  }

  implicit val writeFiniteDuration: ByteBufWriter[FiniteDuration] =
    writeLongAsString.contramap(_.toSeconds)
  //    Unbounded.writeLongAsString.contramap(_.toSeconds)

  implicit val writeDeadline: ByteBufWriter[Deadline] =
    writeLongAsString.contramap(d ⇒ (d.timeLeft.toMillis + System.currentTimeMillis()) / 1000)
  //    Unbounded.writeLongAsString.contramap(d ⇒ d.timeLeft.toMillis + System.currentTimeMillis() / 1000)
}

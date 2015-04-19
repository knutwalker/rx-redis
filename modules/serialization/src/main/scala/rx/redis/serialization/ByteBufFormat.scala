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

import io.netty.buffer.ByteBuf

sealed trait ByteBufFormat[A] extends ByteBufWriter[A] with ByteBufReader[A] {

  def xmap[X](f: A ⇒ X)(g: X ⇒ A): ByteBufFormat[X] =
    ByteBufFormat.from(map(f), contramap(g))

  def xflatMap[X](f: A ⇒ X)(g: X ⇒ TraversableOnce[A]): ByteBufFormat[X] =
    ByteBufFormat.from(map(f), contraflatMap(g))

  def and[B](f: ByteBufFormat[B]): ByteBufFormat[(A, B)] =
    ByteBufFormat.from(andRead(f), andWrite(f))

  def or[B](f: ByteBufFormat[B]): ByteBufFormat[Either[A, B]] =
    ByteBufFormat.from(orRead(f), orWrite(f))
}

object ByteBufFormat {

  def format[A](f: ByteBuf ⇒ A, g: A ⇒ ByteBuf): ByteBufFormat[A] =
    from(ByteBufReader(f), ByteBufWriter.write(g))

  def apply[A](f: ByteBuf ⇒ A, g: (ByteBuf, A) ⇒ ByteBuf): ByteBufFormat[A] =
    from(ByteBufReader(f), ByteBufWriter(g))

  def from[A](implicit reader: ByteBufReader[A], writer: ByteBufWriter[A]): ByteBufFormat[A] = {
    new ByteBufFormat[A] {
      def fromByteBuf(bb: ByteBuf): A = reader.fromByteBuf(bb)
      def toByteBuf(bb: ByteBuf, a: A): ByteBuf = writer.toByteBuf(bb, a)
    }
  }

  def of[A](implicit A: ByteBufFormat[A]): ByteBufFormat[A] = A

  implicit val formatBoolean: ByteBufFormat[Boolean] =
    from(ByteBufReader.readBoolean, ByteBufWriter.writeBoolean)

  implicit val formatByte: ByteBufFormat[Byte] =
    from(ByteBufReader.readByte, ByteBufWriter.writeByte)

  implicit val formatChar: ByteBufFormat[Char] =
    from(ByteBufReader.readChar, ByteBufWriter.writeChar)

  implicit val formatInt: ByteBufFormat[Int] =
    from(ByteBufReader.readInt, ByteBufWriter.writeInt)

  implicit val formatLong: ByteBufFormat[Long] =
    from(ByteBufReader.readLong, ByteBufWriter.writeLong)

  implicit val formatDouble: ByteBufFormat[Double] =
    from(ByteBufReader.readDouble, ByteBufWriter.writeDouble)

  implicit val formatFloat: ByteBufFormat[Float] =
    from(ByteBufReader.readFloat, ByteBufWriter.writeFloat)

  implicit val formatByteArray: ByteBufFormat[Array[Byte]] =
    from(ByteBufReader.readByteArray, ByteBufWriter.writeByteArray)

  implicit val formatString: ByteBufFormat[String] =
    from(ByteBufReader.readString, ByteBufWriter.writeString)

  // Option
  // List / etc...

  val formatFramelessByteArray: ByteBufFormat[Array[Byte]] =
    from(ByteBufReader.readFramelessByteArray, ByteBufWriter.writeFramelessByteArray)

  val formatFramelessString: ByteBufFormat[String] =
    from(ByteBufReader.readFramelessString, ByteBufWriter.writeFramelessString)

  val formatLongAsString: ByteBufFormat[Long] =
    from(ByteBufReader.readLongAsString, ByteBufWriter.writeLongAsString)

}

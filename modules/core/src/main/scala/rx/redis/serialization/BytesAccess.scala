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

import java.nio.charset.Charset
import scala.annotation.implicitNotFound

@implicitNotFound("Need an implicit BytesAccess[${A}] to be able to parse ${A}s.")
trait BytesAccess[A] {

  def markReaderIndex(a: A): Unit

  def resetReaderIndex(a: A): Unit

  def readerIndex(a: A): Int

  def isReadable(a: A, n: Int): Boolean

  def isReadable(a: A): Boolean

  def getByteAt(a: A, n: Int): Byte

  def readNextByte(a: A): Byte

  def readBytes(a: A, n: Int): A

  def skipBytes(a: A, n: Int): Unit

  def bytesBefore(a: A, b: Byte): Int

  def toString(a: A, charset: Charset): String

  def toByteArray(a: A): Array[Byte]

  /////

  def writeByte(a: A, b: Byte): Unit

  def writeBytes(a: A, bs: Array[Byte]): Unit
}

object BytesAccess {
  @inline def apply[A](implicit A: BytesAccess[A]): BytesAccess[A] = A
}

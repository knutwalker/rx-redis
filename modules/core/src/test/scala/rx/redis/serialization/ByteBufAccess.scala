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

import java.nio.charset.Charset

import io.netty.buffer.ByteBuf

trait ByteBufAccess {
  implicit val byteBufAccess = new BytesAccess[ByteBuf] {
    def markReaderIndex(a: ByteBuf): Unit =
      a.markReaderIndex()

    def isReadable(a: ByteBuf, n: Int): Boolean =
      a.isReadable(n)

    def isReadable(a: ByteBuf): Boolean =
      a.isReadable

    def resetReaderIndex(a: ByteBuf): Unit =
      a.resetReaderIndex()

    def readerIndex(a: ByteBuf): Int =
      a.readerIndex()

    def getByteAt(a: ByteBuf, n: Int): Byte =
      a.getByte(n)

    def bytesBefore(a: ByteBuf, b: Byte): Int =
      a.bytesBefore(b)

    def skipBytes(a: ByteBuf, n: Int): Unit =
      a.skipBytes(n)

    def toByteArray(a: ByteBuf): Array[Byte] =
      if (a.hasArray) {
        a.array()
      } else {
        val len = a.readableBytes()
        val ary = new Array[Byte](len)
        a.readBytes(ary)
        ary
      }

    def readNextByte(a: ByteBuf): Byte =
      a.readByte()

    def readBytes(a: ByteBuf, n: Int): ByteBuf =
      a.readBytes(n)

    def toString(a: ByteBuf, charset: Charset): String =
      a.toString(charset)

    def writeByte(a: ByteBuf, b: Byte): BytesAccess[ByteBuf] = {
      a.writeByte(b)
      this
    }

    def writeBytes(a: ByteBuf, bs: Array[Byte]): BytesAccess[ByteBuf] = {
      a.writeBytes(bs)
      this
    }
  }
}

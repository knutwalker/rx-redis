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
package rx.redis.protocol

import io.netty.buffer.{ ByteBuf, ByteBufAllocator }

import rx.redis.resp._
import rx.redis.serialization.BytesFormat

import java.nio.charset.Charset

object Serializer {
  private final val INSTANCE = new Serializer()

  def apply(dt: DataType, bb: ByteBuf): ByteBuf = INSTANCE(dt, bb)

  def apply(dt: DataType, alloc: ByteBufAllocator): ByteBuf = {
    INSTANCE(dt, alloc.buffer())
  }
  def apply(dt: DataType, charset: Charset, alloc: ByteBufAllocator): String = {
    apply(dt, alloc).toString(charset)
  }
}

final class Serializer private () {
  import rx.redis.resp.Protocol._

  private def writeSimpleString(bb: ByteBuf, data: String): Unit = {
    val content = BytesFormat[String].bytes(data)
    bb.writeByte(Plus).writeBytes(content).writeBytes(CrLf)
  }

  private def writeError(bb: ByteBuf, data: String): Unit = {
    val content = BytesFormat[String].bytes(data)
    bb.writeByte(Minus).writeBytes(content).writeBytes(CrLf)
  }

  private def writeInteger(bb: ByteBuf, data: Long): Unit = {
    val content = BytesFormat[Long].bytes(data)
    bb.writeByte(Colon).writeBytes(content).writeBytes(CrLf)
  }

  private def writeArray(bb: ByteBuf, items: Array[DataType]): Unit = {
    val size = BytesFormat[Long].bytes(items.length)
    bb.writeByte(Asterisk).
      writeBytes(size).
      writeBytes(CrLf)
    items.foreach(item ⇒ quickApply(item, bb))
  }

  private def writeBytes(bb: ByteBuf, bytes: Array[Byte]): Unit = {
    bb.writeByte(Dollar).
      writeBytes(BytesFormat[Long].bytes(bytes.length)).
      writeBytes(CrLf).
      writeBytes(bytes).
      writeBytes(CrLf)
  }

  def writeNullString(bb: ByteBuf): Unit = {
    bb.writeByte(Dollar).writeBytes(Nullary).writeBytes(CrLf)
  }

  def writeNullArray(bb: ByteBuf): Unit = {
    bb.writeByte(Asterisk).writeBytes(Nullary).writeBytes(CrLf)
  }
  private def quickApply(data: DataType, bb: ByteBuf): Unit = data match {
    case RespString(s)  ⇒ writeSimpleString(bb, s)
    case RespError(e)   ⇒ writeError(bb, e)
    case RespInteger(l) ⇒ writeInteger(bb, l)
    case RespArray(ds)  ⇒ writeArray(bb, ds)
    case RespBytes(bs)  ⇒ writeBytes(bb, bs)
    case NullString     ⇒ writeNullString(bb)
    case NullArray      ⇒ writeNullArray(bb)
  }

  def apply(data: DataType, bb: ByteBuf): ByteBuf = {
    quickApply(data, bb)
    bb
  }
}

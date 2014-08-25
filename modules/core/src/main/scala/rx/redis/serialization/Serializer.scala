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

import rx.redis.resp._

final class Serializer[A](implicit A: BytesAccess[A]) {
  import rx.redis.resp.Protocol._

  private def writeSimpleString(bb: A, data: String): Unit = {
    val content = BytesFormat[String].bytes(data)
    A.writeByte(bb, Plus).writeBytes(bb, content).writeBytes(bb, CrLf)
  }

  private def writeError(bb: A, data: String): Unit = {
    val content = BytesFormat[String].bytes(data)
    A.writeByte(bb, Minus).writeBytes(bb, content).writeBytes(bb, CrLf)
  }

  private def writeInteger(bb: A, data: Long): Unit = {
    val content = BytesFormat[Long].bytes(data)
    A.writeByte(bb, Colon).writeBytes(bb, content).writeBytes(bb, CrLf)
  }

  private def writeArray(bb: A, items: Array[RespType]): Unit = {
    val size = BytesFormat[Long].bytes(items.length)
    A.writeByte(bb, Asterisk).
      writeBytes(bb, size).
      writeBytes(bb, CrLf)
    items.foreach(item ⇒ quickApply(item, bb))
  }

  private def writeBytes(bb: A, bytes: Array[Byte]): Unit = {
    A.writeByte(bb, Dollar).
      writeBytes(bb, BytesFormat[Long].bytes(bytes.length)).
      writeBytes(bb, CrLf).
      writeBytes(bb, bytes).
      writeBytes(bb, CrLf)
  }

  def writeNullString(bb: A): Unit = {
    A.writeByte(bb, Dollar).writeBytes(bb, Nullary).writeBytes(bb, CrLf)
  }

  def writeNullArray(bb: A): Unit = {
    A.writeByte(bb, Asterisk).writeBytes(bb, Nullary).writeBytes(bb, CrLf)
  }
  private def quickApply(data: RespType, bb: A): Unit = data match {
    case RespString(s)  ⇒ writeSimpleString(bb, s)
    case RespError(e)   ⇒ writeError(bb, e)
    case RespInteger(l) ⇒ writeInteger(bb, l)
    case RespArray(ds)  ⇒ writeArray(bb, ds)
    case RespBytes(bs)  ⇒ writeBytes(bb, bs)
    case NullString     ⇒ writeNullString(bb)
    case NullArray      ⇒ writeNullArray(bb)
  }

  def apply(data: RespType, bb: A): A = {
    quickApply(data, bb)
    bb
  }
}

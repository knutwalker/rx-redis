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

package rx.redis.resp

import rx.redis.util.Utf8

import io.netty.buffer.{ ByteBuf, Unpooled }

import java.nio.charset.Charset

sealed abstract class RespType

case class RespString(data: String) extends RespType {
  require(!data.contains("\r\n"), "A RESP String must not contain [\\r\\n].")
  override def toString: String = data
}

case class RespError(reason: String) extends RespType {
  override def toString: String = reason
}

case class RespInteger(value: Long) extends RespType {
  override def toString: String = value.toString
}

case class RespArray(elements: Vector[RespType]) extends RespType {
  override def toString: String = elements.map(_.toString).mkString("[", ", ", "]")

  def :+(x: RespType): RespArray = RespArray(elements :+ x)
  def +:(x: RespType): RespArray = RespArray(x +: elements)
  def ++(xs: RespArray): RespArray = RespArray(elements ++ xs.elements)
  def ++(xs: RespType*): RespArray = RespArray(elements ++ xs)
}
object RespArray {
  val empty = RespArray(Vector.empty)

  def apply(x: RespType, xs: RespType*): RespArray =
    RespArray(x +: xs.toVector)
}

case class RespBytes private (bb: ByteBuf) extends RespType {

  override def toString: String = bb.toString(Utf8)

  def toString(charset: Charset): String = bb.toString(charset)
}
object RespBytes {
  def wrap(bb: ByteBuf): RespBytes =
    new RespBytes(Unpooled.unmodifiableBuffer(bb))
}

case object NullString extends RespType {
  override def toString: String = "NULL"
}

case object NullArray extends RespType {
  override def toString: String = "NULL"
}

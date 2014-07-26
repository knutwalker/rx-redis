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
package rx.redis.resp

import rx.redis.util.Utf8

import java.nio.charset.Charset
import java.util

sealed abstract class RespType

sealed abstract class DataType extends RespType

case class RespString(data: String) extends DataType {
  require(!data.contains("\r\n"), "A RESP String must not contain [\\r\\n].")
  override def toString: String = data
}

case class RespError(reason: String) extends DataType {
  override def toString: String = reason
}

case class RespInteger(value: Long) extends DataType {
  override def toString: String = value.toString
}

case class RespArray(elements: Array[DataType]) extends DataType {
  override def toString: String = elements.map(_.toString).mkString("[", ", ", "]")

  override def equals(obj: scala.Any): Boolean = obj match {
    case RespArray(other) ⇒
      util.Arrays.equals(elements.asInstanceOf[Array[AnyRef]], other.asInstanceOf[Array[AnyRef]])
    case _ ⇒ super.equals(obj)
  }
}

case class RespBytes(bytes: Array[Byte]) extends DataType {
  override def equals(obj: scala.Any): Boolean = obj match {
    case RespBytes(bs) ⇒ util.Arrays.equals(bytes, bs)
    case _             ⇒ super.equals(obj)
  }

  override def toString: String = new String(bytes, Utf8)

  def toString(charset: Charset): String = new String(bytes, charset)
}
object RespBytes {
  def apply(s: String, charset: Charset): RespBytes =
    apply(s.getBytes(charset))

  def apply(s: String): RespBytes =
    apply(s, Utf8)
}

case object NullString extends DataType {
  override def toString: String = "NULL"
}

case object NullArray extends DataType {
  override def toString: String = "NULL"
}

sealed abstract class ErrorType extends RespType

case object NotEnoughData extends ErrorType {
  override def toString: String = "[INCOMPLETE]"
}
case class ProtocolError(pos: Int, found: Char, expected: List[Byte]) extends ErrorType {
  override def toString: String = {
    val e = expected mkString ", "
    s"Protocol error at char $pos, expected [$e], but found [$found]"
  }
}

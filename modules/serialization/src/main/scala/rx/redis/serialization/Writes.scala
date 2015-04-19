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

import rx.redis.util.Utf8

import io.netty.buffer.{ Unpooled, ByteBuf }

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

@implicitNotFound("Cannot find a Writes of ${A}. You have to implement an rx.redis.serialization.Writes[${A}] in order to send ${A} as a command.")
trait Writes[A] {

  def write(bb: ByteBuf, value: A): ByteBuf
}

object Writes {
  @inline def apply[A](implicit A: Writes[A]): Writes[A] = A

  def writes[A]: Writes[A] = macro WritesMacro.writes[A]

  final implicit class WritesSyntax[A](val x: A) extends AnyVal {

    def writeTo(bb: ByteBuf)(implicit A: Writes[A]): ByteBuf =
      A.write(bb, x)

    def writeToUnpooled(implicit A: Writes[A]): ByteBuf =
      A.write(Unpooled.buffer(), x)

    def writeToString(implicit A: Writes[A]): String = {
      val buf = Unpooled.buffer()
      val res = A.write(buf, x).toString(Utf8)
      buf.release()
      res
    }

  }
}

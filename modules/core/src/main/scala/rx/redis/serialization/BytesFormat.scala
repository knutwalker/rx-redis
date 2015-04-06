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

import java.util.concurrent.TimeUnit
import scala.annotation.implicitNotFound
import scala.concurrent.duration.{ Deadline, FiniteDuration }

@implicitNotFound("Cannot find a BytesFormat of ${A}. You have to implement an rx.redis.serialization.BytesFormat[${A}] in order to use ${A} as custom value.")
trait BytesFormat[A] {

  def bytes(value: A): Array[Byte]

  def value(bytes: Array[Byte]): A
}

object BytesFormat {

  @inline def apply[T](implicit T: BytesFormat[T]): BytesFormat[T] = T

  def from[A: BytesFormat, B](f: B ⇒ A)(g: A ⇒ B): BytesFormat[B] =
    new BytesFormat[B] {
      def bytes(value: B): Array[Byte] =
        BytesFormat[A].bytes(f(value))

      def value(bytes: Array[Byte]): B =
        g(BytesFormat[A].value(bytes))
    }

  implicit val ByteArrayBytes: BytesFormat[Array[Byte]] = new BytesFormat[Array[Byte]] {
    def bytes(value: Array[Byte]): Array[Byte] = value

    def value(bytes: Array[Byte]): Array[Byte] = bytes
  }

  implicit val StringBytes = from((_: String).getBytes(Utf8))(b ⇒ new String(b, Utf8))

  implicit val LongBytes = from((_: Long).toString)(a ⇒ a.toLong)

  val JLongBytes = from((l: java.lang.Long) ⇒ l.longValue())(sl ⇒ sl)

  implicit val DurationBytes = from((_: FiniteDuration).toSeconds)(FiniteDuration(_, TimeUnit.SECONDS))

  implicit val DeadlineBytes = from((d: Deadline) ⇒
    (d.timeLeft.toMillis + System.currentTimeMillis()) / 1000)(t ⇒
    FiniteDuration(t * 1000 - System.currentTimeMillis(), TimeUnit.MILLISECONDS).fromNow)
}

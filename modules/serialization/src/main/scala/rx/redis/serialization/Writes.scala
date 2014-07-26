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

import rx.redis.resp.{ DataType, RespArray, RespBytes }

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

@implicitNotFound("No type class found for ${A}. You have to implement an rx.redis.serialization.Writes[${A}] in order to send ${A} directly.")
trait Writes[A] {

  def write(value: A): DataType
}

object Writes {
  @inline def apply[A](implicit A: Writes[A]): Writes[A] = A

  def writes[A]: Writes[A] = macro Macros.writes[A]

  implicit object DirectStringWrites extends Writes[String] {
    def write(value: String): DataType = {
      val items: Array[DataType] = value.split(' ').map(RespBytes(_))(collection.breakOut)
      RespArray(items)
    }
  }
}

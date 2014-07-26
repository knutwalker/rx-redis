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

import scala.annotation.implicitNotFound

@implicitNotFound("Cannot find a Reads of ${A}. You have to implement an rx.redis.serialization.Reads[${A}] in order to read a ${A}.")
trait Reads[A] {

  def read: PartialFunction[RespType, A]
}

object Reads {
  @inline def apply[A](implicit A: Reads[A]): Reads[A] = A

  implicit val bool: Reads[Boolean] = new Reads[Boolean] {
    val read: PartialFunction[RespType, Boolean] = {
      case RespString(s) if s == "OK" ⇒ true
      case RespInteger(i)             ⇒ i > 0
    }
  }

  implicit val bytes: Reads[Array[Byte]] = new Reads[Array[Byte]] {
    val read: PartialFunction[RespType, Array[Byte]] = {
      case RespBytes(b)  ⇒ b
      case RespString(s) ⇒ BytesFormat[String].bytes(s)
    }
  }

  implicit val int: Reads[Long] = new Reads[Long] {
    val read: PartialFunction[RespType, Long] = {
      case RespInteger(l) ⇒ l
    }
  }

  implicit val list: Reads[List[RespType]] = new Reads[List[RespType]] {
    val read: PartialFunction[RespType, List[RespType]] = {
      case RespArray(items) ⇒ items.toList
    }
  }

  implicit val unzip: Reads[List[(RespType, RespType)]] = new Reads[List[(RespType, RespType)]] {
    val read: PartialFunction[RespType, List[(RespType, RespType)]] = {
      case RespArray(items) ⇒
        items.grouped(2).map(xs ⇒ (xs(0), xs(1))).toList
    }
  }
}

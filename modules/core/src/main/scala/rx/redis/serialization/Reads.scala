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

import scala.annotation.implicitNotFound
import scala.language.experimental.macros
import scala.language.higherKinds

import rx.redis.resp._

@implicitNotFound("Cannot find a Reads of ${A}.")
trait Reads[-A, M[_]] {
  type R
  def read: PartialFunction[RespType, M[R]]
}

object Reads {
  @inline def apply[A, M[_]](implicit A: Reads[A, M]): Reads[A, M] = A

  private val boolPf: PartialFunction[RespType, Boolean] = {
    case RespString(s) if s == "OK" ⇒ true
    case RespInteger(i)             ⇒ i > 0
  }

  private val intPf: PartialFunction[RespType, Long] = {
    case RespInteger(l) ⇒ l
  }

  private def optPf[T](implicit T: BytesFormat[T]): PartialFunction[RespType, Option[T]] = {
    case RespBytes(b) ⇒ Some(T.value(b))
    case NullString   ⇒ None
  }

  private def valuePf[T](implicit T: BytesFormat[T]): PartialFunction[RespType, T] = {
    case RespBytes(b)  ⇒ T.value(b)
    case RespString(s) ⇒ T.value(BytesFormat[String].bytes(s))
  }

  private def listPf[T: BytesFormat]: PartialFunction[RespType, List[T]] = {
    case RespArray(items) ⇒
      items.collect(valuePf[T])(collection.breakOut)
  }

  private def listOptPf[T: BytesFormat]: PartialFunction[RespType, List[Option[T]]] = {
    case RespArray(items) ⇒
      items.collect(optPf[T])(collection.breakOut)
  }

  private def pairPf[T, U](T: PartialFunction[RespType, T], U: PartialFunction[RespType, U]): PartialFunction[RespType, List[(T, U)]] = {
    case RespArray(items) ⇒
      items.grouped(2).collect {
        case Array(t, u) if T.isDefinedAt(t) && U.isDefinedAt(u) ⇒
          T(t) -> U(u)
      }.toList
  }

  private def pairPf[T: BytesFormat, U: BytesFormat]: PartialFunction[RespType, List[(T, U)]] = {
    val tPf = valuePf[T]
    val uPf = valuePf[U]
    pairPf(tPf, uPf)
  }

  def bool[A]: Reads[A, Id] { type R = Boolean } = new Reads[A, Id] {
    type R = Boolean
    val read = boolPf
  }

  def int[A]: Reads[A, Id] { type R = Long } = new Reads[A, Id] {
    type R = Long
    val read = intPf
  }

  def value[A, T: BytesFormat]: Reads[A, Id] { type R = T } = new Reads[A, Id] {
    type R = T
    val read = valuePf[T]
  }

  def opt[A, T: BytesFormat]: Reads[A, Id] { type R = Option[T] } = new Reads[A, Id] {
    type R = Option[T]
    val read = optPf[T]
  }

  def list[A, T: BytesFormat]: Reads[A, List] { type R = T } = new Reads[A, List] {
    type R = T
    val read = listPf[T]
  }

  def listOpt[A, T](implicit T: BytesFormat[T]): Reads[A, List] { type R = Option[T] } = new Reads[A, List] {
    type R = Option[T]
    val read = listOptPf[T]
  }

  def listPair[A, T: BytesFormat, U: BytesFormat]: Reads[A, List] { type R = (T, U) } = new Reads[A, List] {
    type R = (T, U)
    val read = pairPf[T, U]
  }
}

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

import rx.redis.resp._
import rx.redis.util.Utf8

import io.netty.buffer.Unpooled

import scala.annotation.implicitNotFound
import scala.language.experimental.macros
import scala.language.higherKinds

@implicitNotFound("Cannot find a Reads of ${A}.")
trait Reads[-A] {
  type R

  def read: PartialFunction[RespType, TraversableOnce[R]]
}

object Reads {
  type Aux[A, R1] = Reads[A] { type R = R1 }

  @inline def apply[A](implicit A: Reads[A]): Aux[A, A.R] = A

  private[this] val boolPf: PartialFunction[RespType, Boolean] = {
    case RespString(s) if s == "OK" ⇒ true
    case RespInteger(i)             ⇒ i > 0
  }

  private[this] val intPf: PartialFunction[RespType, Long] = {
    case RespInteger(l) ⇒ l
  }

  private[this] def optPf[T](implicit T: ByteBufReader[T]): PartialFunction[RespType, List[Option[T]]] = {
    case b: RespBytes ⇒ Some(T.readAndRelease(b.bb)) :: Nil
    case NullString   ⇒ None :: Nil
  }

  private[this] def valuePf[T](implicit T: ByteBufReader[T]): PartialFunction[RespType, List[T]] = {
    case b: RespBytes  ⇒ T.readAndRelease(b.bb) :: Nil
    case RespString(s) ⇒ T.readAndRelease(Unpooled.copiedBuffer(s, Utf8)) :: Nil
  }

  private[this] def listPf[T: ByteBufReader]: PartialFunction[RespType, Vector[T]] = {
    case RespArray(items) ⇒
      val pf = valuePf[T].andThen(_.head)
      items.collect(pf)
  }

  private[this] def listOptPf[T: ByteBufReader]: PartialFunction[RespType, Vector[Option[T]]] = {
    case RespArray(items) ⇒
      val pf = optPf[T].andThen(_.head)
      items.collect(pf)
  }

  private[this] def pairPf[T, U](T: PartialFunction[RespType, T], U: PartialFunction[RespType, U]): PartialFunction[RespType, Vector[(T, U)]] = {
    case RespArray(items) ⇒
      items.grouped(2).collect {
        case Vector(t, u) if T.isDefinedAt(t) && U.isDefinedAt(u) ⇒
          T(t) -> U(u)
      }.toVector
  }

  private[this] def pairPf[T: ByteBufReader, U: ByteBufReader]: PartialFunction[RespType, Vector[(T, U)]] = {
    val tPf = valuePf[T].andThen(_.head)
    val uPf = valuePf[U].andThen(_.head)
    pairPf(tPf, uPf)
  }

  def bool[A]: Aux[A, Boolean] = new Reads[A] {
    type R = Boolean
    val read = boolPf.andThen(_ :: Nil)
  }

  def int[A]: Aux[A, Long] = new Reads[A] {
    type R = Long
    val read = intPf.andThen(_ :: Nil)
  }

  def value[A, T: ByteBufReader]: Aux[A, T] = new Reads[A] {
    type R = T
    val read = valuePf[T]
  }

  def opt[A, T: ByteBufReader]: Aux[A, Option[T]] = new Reads[A] {
    type R = Option[T]
    val read = optPf[T]
  }

  def list[A, T: ByteBufReader]: Aux[A, T] = new Reads[A] {
    type R = T
    val read = listPf[T]
  }

  def listOpt[A, T](implicit T: ByteBufReader[T]): Aux[A, Option[T]] = new Reads[A] {
    type R = Option[T]
    val read = listOptPf[T]
  }

  def listPair[A, T: ByteBufReader, U: ByteBufReader]: Aux[A, (T, U)] = new Reads[A] {
    type R = (T, U)
    val read = pairPf[T, U]
  }
}

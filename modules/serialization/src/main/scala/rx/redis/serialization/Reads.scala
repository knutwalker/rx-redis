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
import util.control.NoStackTrace

import java.io.{ StringWriter, PrintWriter }

case class MultiException(exs: List[Throwable]) extends Exception with NoStackTrace {
  override def getMessage: String = {
    val stringWriter: StringWriter = new StringWriter
    val writer: PrintWriter = new PrintWriter(stringWriter)
    writer.println("Multiple exceptions occurred, they were:")
    exs.zipWithIndex.foreach {
      case (ex, n) ⇒
        format(n + 1, ex, writer)
    }
    writer.close()
    stringWriter.toString
  }

  private def format(n: Int, ex: Throwable, w: PrintWriter): Unit = {
    w.println(s"$n. ${ex.getMessage}")
    w.print("\t\t")
    ex.printStackTrace(w)
  }
}

@implicitNotFound("Cannot find a Reads of ${A}.")
trait Reads[-A, M[_]] {
  type R
  def read: PartialFunction[RespType, M[R]]
}

object Reads {
  @inline def apply[A, M[_]](implicit A: Reads[A, M]): Reads[A, M] = A

  private[this] val boolPf: PartialFunction[RespType, Boolean] = {
    case RespString(s) if s == "OK" ⇒ true
    case RespInteger(i)             ⇒ i > 0
  }

  private[this] val intPf: PartialFunction[RespType, Long] = {
    case RespInteger(l) ⇒ l
  }

  // TODO: Error Handling
  private[this] def optPf[T](implicit T: ByteBufReader[T]): PartialFunction[RespType, Option[T]] = {
    case b: RespBytes ⇒
      T.readAndRelease(b.bb) match {
        case Left(Nil) ⇒
          throw new IllegalArgumentException("unknown error, this is a bug")
        case Left(ex :: Nil) ⇒
          throw ex
        case Left(exs) ⇒
          throw MultiException(exs)

        case Right(x) ⇒ Some(x)
      }
    case NullString ⇒ None
  }

  // TODO: Error Handling
  private[this] def valuePf[T](implicit T: ByteBufReader[T]): PartialFunction[RespType, T] = {
    case b: RespBytes ⇒
      T.readAndRelease(b.bb) match {
        case Left(xs) ⇒
          throw MultiException(xs)
        case Right(x) ⇒ x
      }
    case RespString(s) ⇒
      T.readAndRelease(Unpooled.copiedBuffer(s, Utf8)) match {
        case Left(xs) ⇒
          throw MultiException(xs)
        case Right(x) ⇒ x
      }
  }

  private[this] def listPf[T: ByteBufReader]: PartialFunction[RespType, List[T]] = {
    case RespArray(items) ⇒
      val pf = valuePf[T]
      items.collect(pf)(collection.breakOut)
  }

  private[this] def listOptPf[T: ByteBufReader]: PartialFunction[RespType, List[Option[T]]] = {
    case RespArray(items) ⇒
      val pf = optPf[T]
      items.collect(pf)(collection.breakOut)
  }

  private[this] def pairPf[T, U](T: PartialFunction[RespType, T], U: PartialFunction[RespType, U]): PartialFunction[RespType, List[(T, U)]] = {
    case RespArray(items) ⇒
      items.grouped(2).collect {
        case Vector(t, u) if T.isDefinedAt(t) && U.isDefinedAt(u) ⇒
          T(t) -> U(u)
      }.toList
  }

  private[this] def pairPf[T: ByteBufReader, U: ByteBufReader]: PartialFunction[RespType, List[(T, U)]] = {
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

  def value[A, T: ByteBufReader]: Reads[A, Id] { type R = T } = new Reads[A, Id] {
    type R = T
    val read = valuePf[T]
  }

  def opt[A, T: ByteBufReader]: Reads[A, Id] { type R = Option[T] } = new Reads[A, Id] {
    type R = Option[T]
    val read = optPf[T]
  }

  def list[A, T: ByteBufReader]: Reads[A, List] { type R = T } = new Reads[A, List] {
    type R = T
    val read = listPf[T]
  }

  def listOpt[A, T](implicit T: ByteBufReader[T]): Reads[A, List] { type R = Option[T] } = new Reads[A, List] {
    type R = Option[T]
    val read = listOptPf[T]
  }

  def listPair[A, T: ByteBufReader, U: ByteBufReader]: Reads[A, List] { type R = (T, U) } = new Reads[A, List] {
    type R = (T, U)
    val read = pairPf[T, U]
  }
}

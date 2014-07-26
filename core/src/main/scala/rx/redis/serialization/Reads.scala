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

import rx.Observable
import rx.functions.Func1

import rx.redis.resp.{ RespArray, RespBytes, RespInteger, RespString, RespType }

import scala.collection.JavaConverters._

trait Reads[T] { self ⇒

  def pf: PartialFunction[RespType, T]

  val asObservable: Func1[_ >: RespType, _ <: Observable[T]] =
    new Func1[RespType, Observable[T]] {
      def call(t1: RespType): Observable[T] = {
        if (self.pf.isDefinedAt(t1))
          Observable.just(self.pf(t1))
        else
          Observable.empty()
      }
    }

  def asObservableOfA[A](implicit A: BytesFormat[A], ev: T =:= Array[Byte]): Func1[_ >: RespType, _ <: Observable[A]] =
    new Func1[RespType, Observable[A]] {
      def call(t1: RespType): Observable[A] = {
        if (self.pf.isDefinedAt(t1))
          Observable.just(A.value(self.pf(t1)))
        else
          Observable.empty()
      }
    }

  def asOptionObservableOfA[A](implicit A: BytesFormat[A], ev: T =:= Array[Byte]): Func1[_ >: RespType, _ <: Observable[Option[A]]] =
    new Func1[RespType, Observable[Option[A]]] {
      def call(t1: RespType): Observable[Option[A]] = {
        if (self.pf.isDefinedAt(t1))
          Observable.just(Some(A.value(self.pf(t1))))
        else
          Observable.just(None)
      }
    }

  def asManyObservableOfA[A](implicit A: BytesFormat[A], ev: T <:< List[RespType]): Func1[_ >: RespType, _ <: Observable[A]] =
    new Func1[RespType, Observable[A]] {
      def call(t1: RespType): Observable[A] = {
        if (self.pf.isDefinedAt(t1))
          Observable.from(self.pf(t1).collect(Reads.bytes.pf).map(A.value).asJava)
        else
          Observable.empty()
      }
    }

  def asManyOptionObservableOfA[A](implicit A: BytesFormat[A], ev: T <:< List[RespType]): Func1[_ >: RespType, _ <: Observable[Option[A]]] =
    new Func1[RespType, Observable[Option[A]]] {
      def call(t1: RespType): Observable[Option[A]] = {
        if (self.pf.isDefinedAt(t1))
          Observable.from(self.pf(t1).map(Reads.bytes.pf.andThen(A.value).lift).asJava)
        else
          Observable.empty()
      }
    }

  def asObservableOfAAndB[A, B](implicit A: BytesFormat[A], B: BytesFormat[B], ev: T <:< List[(RespType, RespType)]): Func1[_ >: RespType, _ <: Observable[(A, B)]] =
    new Func1[RespType, Observable[(A, B)]] {
      def call(t1: RespType): Observable[(A, B)] = {
        if (self.pf.isDefinedAt(t1))
          Observable.from(self.pf(t1).collect {
            case (x, y) if Reads.bytes.pf.isDefinedAt(x) && Reads.bytes.pf.isDefinedAt(y) ⇒
              (A.value(Reads.bytes.pf(x)), B.value(Reads.bytes.pf(y)))
          }.asJava)
        else
          Observable.empty()
      }
    }
}

object Reads {
  @inline def apply[A](implicit A: Reads[A]): Reads[A] = A

  implicit val bool: Reads[Boolean] = new Reads[Boolean] {
    val pf: PartialFunction[RespType, Boolean] = {
      case RespString(s) if s == "OK" ⇒ true
      case RespInteger(i)             ⇒ i > 0
    }
  }

  implicit val bytes: Reads[Array[Byte]] = new Reads[Array[Byte]] {
    val pf: PartialFunction[RespType, Array[Byte]] = {
      case RespBytes(b)  ⇒ b
      case RespString(s) ⇒ BytesFormat[String].bytes(s)
    }
  }

  implicit val int: Reads[Long] = new Reads[Long] {
    val pf: PartialFunction[RespType, Long] = {
      case RespInteger(l) ⇒ l
    }
  }

  implicit val list: Reads[List[RespType]] = new Reads[List[RespType]] {
    val pf: PartialFunction[RespType, List[RespType]] = {
      case RespArray(items) ⇒ items.toList
    }
  }

  implicit val unzip: Reads[List[(RespType, RespType)]] = new Reads[List[(RespType, RespType)]] {
    val pf: PartialFunction[RespType, List[(RespType, RespType)]] = {
      case RespArray(items) ⇒
        items.grouped(2).map(xs ⇒ (xs(0), xs(1))).toList
    }
  }
}

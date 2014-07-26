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

package rx.redis.commands

import rx.Observable
import rx.functions.Func1
import rx.redis.resp.RespType
import rx.redis.serialization.{ BytesFormat, Reads }

import scala.collection.JavaConverters._

object ReadsTransformers {

  def asObservable[T](implicit T: Reads[T]): Func1[_ >: RespType, _ <: Observable[T]] =
    new Func1[RespType, Observable[T]] {
      def call(t1: RespType): Observable[T] = {
        if (T.read.isDefinedAt(t1))
          Observable.just(T.read(t1))
        else
          Observable.empty()
      }
    }

  def asObservableOf[T, A](implicit A: BytesFormat[A], r: Reads[T], ev: T =:= Array[Byte]): Func1[_ >: RespType, _ <: Observable[A]] =
    new Func1[RespType, Observable[A]] {
      def call(t1: RespType): Observable[A] = {
        if (r.read.isDefinedAt(t1))
          Observable.just(A.value(r.read(t1)))
        else
          Observable.empty()
      }
    }

  def asObservableOfOptionOf[T, A](implicit A: BytesFormat[A], T: Reads[T], ev: T =:= Array[Byte]): Func1[_ >: RespType, _ <: Observable[Option[A]]] =
    new Func1[RespType, Observable[Option[A]]] {
      def call(t1: RespType): Observable[Option[A]] = {
        if (T.read.isDefinedAt(t1))
          Observable.just(Some(A.value(T.read(t1))))
        else
          Observable.just(None)
      }
    }

  def manyAsObservableOf[T, A](implicit A: BytesFormat[A], T: Reads[T], ev: T <:< List[RespType]): Func1[_ >: RespType, _ <: Observable[A]] =
    new Func1[RespType, Observable[A]] {
      def call(t1: RespType): Observable[A] = {
        if (T.read.isDefinedAt(t1))
          Observable.from(T.read(t1).collect(Reads.bytes.read).map(A.value).asJava)
        else
          Observable.empty()
      }
    }

  def manyAsObservableOfOptionOf[T, A](implicit A: BytesFormat[A], T: Reads[T], ev: T <:< List[RespType]): Func1[_ >: RespType, _ <: Observable[Option[A]]] =
    new Func1[RespType, Observable[Option[A]]] {
      def call(t1: RespType): Observable[Option[A]] = {
        if (T.read.isDefinedAt(t1))
          Observable.from(T.read(t1).map(Reads.bytes.read.andThen(A.value).lift).asJava)
        else
          Observable.empty()
      }
    }

  def manyAsObservableOfPair[T, A, B](implicit A: BytesFormat[A], B: BytesFormat[B], r: Reads[T], ev: T <:< List[(RespType, RespType)]): Func1[_ >: RespType, _ <: Observable[(A, B)]] =
    new Func1[RespType, Observable[(A, B)]] {
      def call(t1: RespType): Observable[(A, B)] = {
        if (r.read.isDefinedAt(t1))
          Observable.from(r.read(t1).collect {
            case (x, y) if Reads.bytes.read.isDefinedAt(x) && Reads.bytes.read.isDefinedAt(y) ⇒
              (A.value(Reads.bytes.read(x)), B.value(Reads.bytes.read(y)))
          }.asJava)
        else
          Observable.empty()
      }
    }
}

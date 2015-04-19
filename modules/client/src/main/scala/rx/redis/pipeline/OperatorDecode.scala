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

package rx.redis.pipeline

import rx.redis.pipeline.OperatorDecode.SubscriberDecode
import rx.redis.resp.RespType
import rx.redis.serialization.{ DecodeException, Reads }

import rx.Observable.Operator
import rx.Subscriber

import scala.util.control.NonFatal

final class OperatorDecode[A, B](A: Reads.Aux[A, B]) extends Operator[B, RespType] {
  def call(downstream: Subscriber[_ >: B]): Subscriber[RespType] =
    new SubscriberDecode[A, B](downstream, A)
}
object OperatorDecode {
  final class SubscriberDecode[A, B](downstream: Subscriber[_ >: B], A: Reads.Aux[A, B]) extends Subscriber[RespType] {
    def onNext(x: RespType): Unit = try {
      A.read
        .andThen(_.foreach(downstream.onNext))
        .applyOrElse(x, (x: RespType) ⇒ downstream.onError(DecodeException[A](x)))
    } catch {
      case NonFatal(ex) ⇒ downstream.onError(DecodeException[A](x, ex))
    }

    def onError(e: Throwable): Unit =
      downstream.onError(e)

    def onCompleted(): Unit =
      downstream.onCompleted()
  }
}

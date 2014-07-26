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

import rx.redis.resp.RespType
import rx.redis.serialization.{ BytesFormat, Writes }

import scala.concurrent.duration.FiniteDuration

case class Get(key: String)
object Get {
  implicit val writes = Writes.writes[Get]
  def reads[A: BytesFormat] = ReadsTransformers.asObservableOfOptionOf[Array[Byte], A] // Reads.bytes.asOptionObservableOfA[A]
}

case class Set[A: BytesFormat](key: String, value: A)
object Set {
  implicit def writes[A: BytesFormat] = Writes.writes[Set[A]]
  val reads = ReadsTransformers.asObservable[Boolean] // Reads.bool.asObservable
}

case class SetEx[A: BytesFormat](key: String, expires: FiniteDuration, value: A)
object SetEx {
  implicit def writes[A: BytesFormat] = Writes.writes[SetEx[A]]
  val reads = ReadsTransformers.asObservable[Boolean] // Reads.bool.asObservable
}

case class SetNx[A: BytesFormat](key: String, value: A)
object SetNx {
  implicit def writes[A: BytesFormat] = Writes.writes[SetNx[A]]
  val reads = ReadsTransformers.asObservable[Boolean] // Reads.bool.asObservable
}

case class Incr(key: String)
object Incr {
  implicit val writes = Writes.writes[Incr]
  val reads = ReadsTransformers.asObservable[Long] // Reads.int.asObservable
}

case class Decr(key: String)
object Decr {
  implicit val writes = Writes.writes[Decr]
  val reads = ReadsTransformers.asObservable[Long] // Reads.int.asObservable
}

case class IncrBy(key: String, amount: Long)
object IncrBy {
  implicit val writes = Writes.writes[IncrBy]
  val reads = ReadsTransformers.asObservable[Long] // Reads.int.asObservable
}

case class DecrBy(key: String, amount: Long)
object DecrBy {
  implicit val writes = Writes.writes[DecrBy]
  val reads = ReadsTransformers.asObservable[Long] // Reads.int.asObservable
}

case class MGet(keys: String*)
object MGet {
  implicit val writes = Writes.writes[MGet]
  def reads[A: BytesFormat] = ReadsTransformers.manyAsObservableOfOptionOf[List[RespType], A] // Reads.list.asManyOptionObservableOfA[A]
}

case class MSet[A: BytesFormat](keys: (String, A)*)
object MSet {
  implicit def writes[A: BytesFormat] = Writes.writes[MSet[A]]
  val reads = ReadsTransformers.asObservable[Boolean] // Reads.bool.asObservable
}

case class StrLen(key: String)
object StrLen {
  implicit val writes = Writes.writes[StrLen]
  val reads = ReadsTransformers.asObservable[Long] // Reads.int.asObservable
}

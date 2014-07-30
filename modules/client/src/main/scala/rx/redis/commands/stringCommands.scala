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

import scala.concurrent.duration.FiniteDuration

import rx.redis.serialization.{ BytesFormat, Reads, Writes }

case class Get(key: String)
object Get {
  implicit val writes = Writes.writes[Get]
  implicit def readsFormat[A: BytesFormat] = Reads.opt[Get, A]
}

case class Set[A: BytesFormat](key: String, value: A)
object Set {
  implicit def writes[A: BytesFormat] = Writes.writes[Set[A]]
  implicit val readsFormat = Reads.bool[Set[_]]
}

case class SetEx[A: BytesFormat](key: String, expires: FiniteDuration, value: A)
object SetEx {
  implicit def writes[A: BytesFormat] = Writes.writes[SetEx[A]]
  implicit val readsFormat = Reads.bool[SetEx[_]]
}

case class SetNx[A: BytesFormat](key: String, value: A)
object SetNx {
  implicit def writes[A: BytesFormat] = Writes.writes[SetNx[A]]
  implicit val readsFormat = Reads.bool[SetNx[_]]
}

case class Incr(key: String)
object Incr {
  implicit val writes = Writes.writes[Incr]
  implicit val readsFormat = Reads.int[Incr]
}

case class Decr(key: String)
object Decr {
  implicit val writes = Writes.writes[Decr]
  implicit val readsFormat = Reads.int[Decr]
}

case class IncrBy(key: String, amount: Long)
object IncrBy {
  implicit val writes = Writes.writes[IncrBy]
  implicit val readsFormat = Reads.int[IncrBy]
}

case class DecrBy(key: String, amount: Long)
object DecrBy {
  implicit val writes = Writes.writes[DecrBy]
  implicit val readsFormat = Reads.int[DecrBy]
}

case class MGet(keys: String*)
object MGet {
  implicit val writes = Writes.writes[MGet]
  implicit def readsFormat[A: BytesFormat] = Reads.listOpt[MGet, A]
}

case class MSet[A: BytesFormat](keys: (String, A)*)
object MSet {
  implicit def writes[A: BytesFormat] = Writes.writes[MSet[A]]
  implicit val readsFormat = Reads.bool[MSet[_]]
}

case class StrLen(key: String)
object StrLen {
  implicit val writes = Writes.writes[StrLen]
  implicit val readsFormat = Reads.int[StrLen]
}

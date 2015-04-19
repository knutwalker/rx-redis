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

package rx.redis.commands

import rx.redis.serialization.{ ByteBufReader, ByteBufWriter, Reads, Writes }

import scala.concurrent.duration.FiniteDuration

case class Get(key: String)
object Get {
  implicit val writes: Writes[Get] =
    Writes.writes[Get]

  implicit def readsFormat[A: ByteBufReader]: Reads.Aux[Get, Option[A]] =
    Reads.opt[Get, A]
}

case class Set[A: ByteBufWriter](key: String, value: A)
object Set {
  implicit def writes[A: ByteBufWriter]: Writes[Set[A]] =
    Writes.writes[Set[A]]

  implicit val readsFormat: Reads.Aux[Set[_], Boolean] =
    Reads.bool[Set[_]]
}

case class SetEx[A: ByteBufWriter](key: String, expires: FiniteDuration, value: A)
object SetEx {
  implicit def writes[A: ByteBufWriter]: Writes[SetEx[A]] =
    Writes.writes[SetEx[A]]

  implicit val readsFormat: Reads.Aux[SetEx[_], Boolean] =
    Reads.bool[SetEx[_]]
}

case class SetNx[A: ByteBufWriter](key: String, value: A)
object SetNx {
  implicit def writes[A: ByteBufWriter]: Writes[SetNx[A]] =
    Writes.writes[SetNx[A]]

  implicit val readsFormat: Reads.Aux[SetNx[_], Boolean] =
    Reads.bool[SetNx[_]]
}

case class Incr(key: String)
object Incr {
  implicit val writes: Writes[Incr] =
    Writes.writes[Incr]

  implicit val readsFormat: Reads.Aux[Incr, Long] =
    Reads.int[Incr]
}

case class Decr(key: String)
object Decr {
  implicit val writes: Writes[Decr] =
    Writes.writes[Decr]

  implicit val readsFormat: Reads.Aux[Decr, Long] =
    Reads.int[Decr]
}

case class IncrBy(key: String, amount: Long)
object IncrBy {
  implicit val writes: Writes[IncrBy] =
    Writes.writes[IncrBy]

  implicit val readsFormat: Reads.Aux[IncrBy, Long] =
    Reads.int[IncrBy]
}

case class DecrBy(key: String, amount: Long)
object DecrBy {
  implicit val writes: Writes[DecrBy] =
    Writes.writes[DecrBy]

  implicit val readsFormat: Reads.Aux[DecrBy, Long] =
    Reads.int[DecrBy]
}

case class MGet(keys: String*)
object MGet {
  implicit val writes: Writes[MGet] =
    Writes.writes[MGet]

  implicit def readsFormat[A: ByteBufReader]: Reads.Aux[MGet, Option[A]] =
    Reads.listOpt[MGet, A]
}

case class MSet[A: ByteBufWriter](keys: (String, A)*)
object MSet {
  implicit def writes[A: ByteBufWriter]: Writes[MSet[A]] =
    Writes.writes[MSet[A]]

  implicit val readsFormat: Reads.Aux[MSet[_], Boolean] =
    Reads.bool[MSet[_]]
}

case class StrLen(key: String)
object StrLen {
  implicit val writes: Writes[StrLen] =
    Writes.writes[StrLen]

  implicit val readsFormat: Reads.Aux[StrLen, Long] =
    Reads.int[StrLen]
}

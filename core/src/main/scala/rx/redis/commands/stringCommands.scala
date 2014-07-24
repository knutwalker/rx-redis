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

import rx.redis.serialization.{BytesFormat, Writes}

import scala.concurrent.duration.FiniteDuration


case class Get(key: String)
object Get {
  implicit val GetWrites = Writes.writes[Get]
}


case class Set[A: BytesFormat](key: String, value: A)
object Set {
  implicit def SetWrites[A: BytesFormat] = Writes.writes[Set[A]]
}


case class SetEx[A: BytesFormat](key: String, expires: FiniteDuration, value: A)
object SetEx {
  implicit def SetExWrites[A: BytesFormat] = Writes.writes[SetEx[A]]
}


case class SetNx[A: BytesFormat](key: String, value: A)
object SetNx {
  implicit def SetNxWrites[A: BytesFormat] = Writes.writes[SetNx[A]]
}


case class Incr(key: String)
object Incr {
  implicit val IncrWrites = Writes.writes[Incr]
}


case class Decr(key: String)
object Decr {
  implicit val DecrWrites = Writes.writes[Decr]
}


case class IncrBy(key: String, amount: Long)
object IncrBy {
  implicit val IncrByWrites = Writes.writes[IncrBy]
}


case class DecrBy(key: String, amount: Long)
object DecrBy {
  implicit val DecrByWrites = Writes.writes[DecrBy]
}


case class MGet(keys: String*)
object MGet {
  implicit val MGetWrites = Writes.writes[MGet]
}


case class MSet[A: BytesFormat](keys: (String, A)*)
object MSet {
  implicit def MSetWrites[A: BytesFormat] = Writes.writes[MSet[A]]
}


case class StrLen(key: String)
object StrLen {
  implicit val StrLenWrites = Writes.writes[StrLen]
}

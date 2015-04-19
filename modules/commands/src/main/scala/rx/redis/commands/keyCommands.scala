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

import scala.concurrent.duration.{ Deadline, FiniteDuration }

import rx.redis.serialization.{ ByteBufReader, Reads, Writes }

case class Del(keys: String*)
object Del {
  implicit val writes: Writes[Del] =
    Writes.writes[Del]

  implicit val readsFormat: Reads.Aux[Del, Long] =
    Reads.int[Del]
}

case class Exists(key: String)
object Exists {
  implicit val writes: Writes[Exists] =
    Writes.writes[Exists]

  implicit val readsFormat: Reads.Aux[Exists, Boolean] =
    Reads.bool[Exists]
}

case class Expire(key: String, expires: FiniteDuration)
object Expire {
  implicit val writes: Writes[Expire] =
    Writes.writes[Expire]

  implicit val readsFormat: Reads.Aux[Expire, Boolean] =
    Reads.bool[Expire]
}

// TODO: Deadline is not a good choice, more something like Joda time
case class ExpireAt(key: String, deadline: Deadline)
object ExpireAt {
  implicit val writes: Writes[ExpireAt] =
    Writes.writes[ExpireAt]

  implicit val readsFormat: Reads.Aux[ExpireAt, Boolean] =
    Reads.bool[ExpireAt]
}

case class Keys(pattern: String)
object Keys {
  implicit val writes: Writes[Keys] =
    Writes.writes[Keys]

  implicit val readsFormat: Reads.Aux[Keys, String] =
    Reads.list[Keys, String](ByteBufReader.readFramelessString)
}

case object RandomKey {
  implicit val writes: Writes[RandomKey.type] =
    Writes.writes[RandomKey.type]

  implicit val readsFormat: Reads.Aux[RandomKey.type, Option[String]] =
    Reads.opt[RandomKey.type, String](ByteBufReader.readFramelessString)
}

case class Ttl(key: String)
object Ttl {
  implicit val writes: Writes[Ttl] =
    Writes.writes[Ttl]

  implicit val readsFormat: Reads.Aux[Ttl, Long] =
    Reads.int[Ttl]
}

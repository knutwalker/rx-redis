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
import rx.redis.serialization.Writes

import scala.concurrent.duration.{ Deadline, FiniteDuration }

case class Del(keys: String*)
object Del {
  implicit val writes = Writes.writes[Del]
  val reads = ReadsTransformers.asObservable[Long] // Reads.int.asObservable
}

case class Exists(key: String)
object Exists {
  implicit val writes = Writes.writes[Exists]
  val reads = ReadsTransformers.asObservable[Boolean] // Reads.bool.asObservable
}

case class Expire(key: String, expires: FiniteDuration)
object Expire {
  implicit val writes = Writes.writes[Expire]
  val reads = ReadsTransformers.asObservable[Boolean] // Reads.bool.asObservable
}

// TODO: Deadline is not a good choice, more something like Joda time
case class ExpireAt(key: String, deadline: Deadline)
object ExpireAt {
  implicit val writes = Writes.writes[ExpireAt]
  val reads = ReadsTransformers.asObservable[Boolean] // Reads.bool.asObservable
}

case class Keys(pattern: String)
object Keys {
  implicit val writes = Writes.writes[Keys]
  val reads = ReadsTransformers.manyAsObservableOf[List[RespType], String] // Reads.list.asManyObservableOfA[String]
}

case object RandomKey {
  implicit val writes = Writes.writes[RandomKey.type]
  val reads = ReadsTransformers.asObservableOfOptionOf[Array[Byte], String] // Reads.bytes.asOptionObservableOfA[String]
}

case class Ttl(key: String)
object Ttl {
  implicit val writes = Writes.writes[Ttl]
  val reads = ReadsTransformers.asObservable[Long] // Reads.int.asObservable
}

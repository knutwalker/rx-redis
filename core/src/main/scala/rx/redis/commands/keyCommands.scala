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

import rx.redis.serialization.Writes

import scala.concurrent.duration.{Deadline, FiniteDuration}


case class Del(keys: String*)
object Del {
  implicit val DelWrites = Writes.writes[Del]
}


case class Exists(key: String)
object Exists {
  implicit val ExistsWrites = Writes.writes[Exists]
}


case class Expire(key: String, expires: FiniteDuration)
object Expire {
  implicit val ExpireWrites = Writes.writes[Expire]
}


// TODO: Deadline is not a good choice, more something like Joda time
case class ExpireAt(key: String, deadline: Deadline)
object ExpireAt {
  implicit val ExpireAtWrites = Writes.writes[ExpireAt]
}


case class Keys(pattern: String)
object Keys {
  implicit val KeysWrites = Writes.writes[Keys]
}


case object RandomKey {
  implicit val RandomKeyWrites = Writes.writes[RandomKey.type]
}

case class Ttl(key: String)
object Ttl {
  implicit val TtlWrites = Writes.writes[Ttl]
}

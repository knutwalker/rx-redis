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

import rx.redis.serialization.{ BytesFormat, Reads, Writes }

case object Ping {
  implicit val writes: Writes[Ping.type] = Writes.writes[Ping.type]
  implicit val readsFormat = Reads.value[Ping.type, String]
}

case class Echo[A: BytesFormat](value: A)
object Echo {
  implicit def writes[A: BytesFormat] = Writes.writes[Echo[A]]
  implicit def readsFormat[A: BytesFormat] = Reads.value[Echo[A], A]
}

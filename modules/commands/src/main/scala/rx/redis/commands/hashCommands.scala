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

case class HGet(key: String, field: String)
object HGet {
  implicit val writes = Writes.writes[HGet]
  implicit def readsFormat[A: BytesFormat] = Reads.opt[HGet, A]
}

case class HGetAll(key: String)
object HGetAll {
  implicit val writes = Writes.writes[HGetAll]
  implicit def readsFormat[A: BytesFormat] = Reads.listPair[HGetAll, String, A]
}

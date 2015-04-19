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

package rx.redis.clients

import rx.redis.commands._
import rx.redis.serialization.{ ByteBufReader, ByteBufWriter }

import rx.Observable

import concurrent.duration.FiniteDuration

trait StringCommands { this: GenericClient ⇒

  final def decr(key: String): Observable[Long] =
    run(Decr(key))

  final def decrBy(key: String, amount: Long): Observable[Long] =
    run(DecrBy(key, amount))

  final def get[A: ByteBufReader](key: String): Observable[Option[A]] =
    run(Get(key))

  final def incr(key: String): Observable[Long] =
    run(Incr(key))

  final def incrBy(key: String, amount: Long): Observable[Long] =
    run(IncrBy(key, amount))

  final def mget[A: ByteBufReader](keys: String*): Observable[Option[A]] =
    run(MGet(keys: _*))

  final def mset[A: ByteBufWriter](items: (String, A)*): Observable[Boolean] =
    run(MSet(items: _*))

  final def set[A: ByteBufWriter](key: String, value: A): Observable[Boolean] =
    run(Set(key, value))

  final def setEx[A: ByteBufWriter](key: String, value: A, expires: FiniteDuration): Observable[Boolean] =
    run(SetEx(key, expires, value))

  final def setNx[A: ByteBufWriter](key: String, value: A): Observable[Boolean] =
    run(SetNx(key, value))

  final def strLen(key: String): Observable[Long] =
    run(StrLen(key))

}

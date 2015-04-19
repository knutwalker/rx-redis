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

import rx.redis.commands.{ Del, Exists, Expire, ExpireAt, Keys, RandomKey, Ttl }

import rx.Observable

import concurrent.duration.{ Deadline, FiniteDuration }

trait KeyCommands { this: GenericClient ⇒

  final def del(keys: String*): Observable[Long] =
    run(Del(keys: _*))

  final def exists(key: String): Observable[Boolean] =
    run(Exists(key))

  final def expire(key: String, expires: FiniteDuration): Observable[Boolean] =
    run(Expire(key, expires))

  final def expireAt(key: String, deadline: Deadline): Observable[Boolean] =
    run(ExpireAt(key, deadline))

  final def keys(pattern: String): Observable[String] =
    run(Keys(pattern))

  final def randomKey(): Observable[Option[String]] =
    run(RandomKey)

  final def ttl(key: String): Observable[Long] =
    run(Ttl(key))

}

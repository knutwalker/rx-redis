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

package rx.redis

import scala.util.{ Try, Properties }

import java.nio.charset.{ Charset, StandardCharsets }
import java.util.Locale

package object util {

  val Utf8: Charset =
    StandardCharsets.UTF_8

  val RedisHost: Option[String] =
    prop("rx.redis.host")

  val RedisPort: Option[Int] =
    prop("rx.redis.port").flatMap(p ⇒ Try(p.toInt).toOption)

  val DefaultRedisHost: String =
    RedisHost.getOrElse("127.0.0.1")

  val DefaultRedisPort: Int =
    RedisPort.getOrElse(6379)

  def prop(name: String): Option[String] = {
    def upper: String = name.split('.').map(_.toUpperCase(Locale.ENGLISH)).mkString("_")
    Properties.propOrNone(name).orElse(Properties.envOrNone(upper))
  }
}

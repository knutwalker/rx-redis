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

import java.nio.charset.StandardCharsets

import rx.redis.resp.{DataType, RespArray, RespBytes}
import rx.redis.serialization.Writes

import org.scalatest.FunSuite

class CommandsSuite extends FunSuite {

  val charset = StandardCharsets.UTF_8

  protected def pretty(s: String, snip: Option[Int] = None) =
    Some(s.replaceAllLiterally("\r\n", "\\r\\n")).map(s ⇒ snip.fold(s)(s.take)).get

  protected def ser[A: Writes](c: A, expected: DataType) = {
    val buf = Writes[A].write(c)
    assert(buf == expected)
  }

  protected def sers[A: Writes](c: A, expectedParts: String*) =
    ser(c, RespArray(expectedParts.map(RespBytes(_)).toArray))
}

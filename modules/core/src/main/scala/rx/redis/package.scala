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

package rx

package object redis {

  implicit class CommandQuote(val ctx: StringContext) extends AnyVal {
    def cmd(args: String*): String with RedisCommand = {
      val strings = ctx.parts.iterator
      val expressions = args.iterator
      val result = strings.
        zipAll(expressions, "", "").
        map { case (a, b) ⇒ a + b }.
        foldLeft("")(_ + _).
        replaceAllLiterally("\\r\\n", "\r\n")

      RedisCommand(result)
    }
  }
}

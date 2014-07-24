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
package rx.redis.resp

private[redis] object Protocol {

  final val Plus = '+'.toByte
  final val Minus = '-'.toByte
  final val Colon = ':'.toByte
  final val Dollar = '$'.toByte
  final val Asterisk = '*'.toByte

  final val typeChars = List(Plus, Minus, Colon, Dollar, Asterisk)

  final val Cr = '\r'.toByte
  final val Lf = '\n'.toByte

  final val CrLf = Array(Cr, Lf)
  final val Nullary = Array(Minus, '1'.toByte)
}

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

import rx.redis.serialization.ByteBufWriter._
import rx.redis.util.Utf8

import io.netty.buffer.{ Unpooled, ByteBuf }

package object serialization {

  type Id[A] = A

  implicit def writeStringAsRedisCommand(s: String with RedisCommand): ByteBuf = {
    val result = s.split(' ')

    val buf = Unpooled.buffer()
    buf.writeByte('*'.toInt)
    writeLongAsString.toByteBuf(buf, result.length.toLong)
    buf.writeByte('\r'.toInt)
    buf.writeByte('\n'.toInt)

    result.foldLeft(buf) { (bb, op) ⇒
      bb.writeByte('$'.toInt)
      writeLongAsString.toByteBuf(bb, op.length.toLong)
      buf.writeByte('\r'.toInt)
      buf.writeByte('\n'.toInt)
      bb.writeBytes(op.getBytes(Utf8))
      buf.writeByte('\r'.toInt)
      buf.writeByte('\n'.toInt)
    }
  }
}

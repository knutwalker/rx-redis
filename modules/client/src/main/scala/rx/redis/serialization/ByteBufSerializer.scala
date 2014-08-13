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

package rx.redis.serialization

import io.netty.buffer.{ ByteBuf, ByteBufAllocator }

import rx.redis.resp.DataType

import java.nio.charset.Charset

object ByteBufSerializer {
  private final val INSTANCE = new Serializer[ByteBuf]()(ByteBufAccess)

  def apply(dt: DataType, bb: ByteBuf): ByteBuf = INSTANCE(dt, bb)

  def apply(dt: DataType, alloc: ByteBufAllocator): ByteBuf = {
    INSTANCE(dt, alloc.buffer())
  }
  def apply(dt: DataType, charset: Charset, alloc: ByteBufAllocator): String = {
    apply(dt, alloc).toString(charset)
  }
}

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
package rx.redis.protocol

import java.util

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import rx.redis.resp.DataType

private[redis] class RespCodec extends StrictByteToMessageCodec[ByteBuf, DataType] {

  def encode(ctx: ChannelHandlerContext, msg: DataType, out: ByteBuf): Unit = {
    Serializer(msg, out)
  }

  def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    Deserializer.foreach(in)(out.add(_))
  }
}

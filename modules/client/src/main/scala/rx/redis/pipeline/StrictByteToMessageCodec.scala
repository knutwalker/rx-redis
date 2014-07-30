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

package rx.redis.pipeline

import scala.reflect.ClassTag

import io.netty.buffer.ByteBuf
import io.netty.channel.{ ChannelHandlerContext, ChannelPromise }
import io.netty.handler.codec.ByteToMessageCodec

abstract class StrictByteToMessageCodec[I <: ByteBuf: ClassTag, O: ClassTag] extends ByteToMessageCodec[O] {

  private final val iClass = implicitly[ClassTag[I]].runtimeClass
  private final val oClass = implicitly[ClassTag[O]].runtimeClass

  override def acceptOutboundMessage(msg: Any): Boolean = {
    oClass.isInstance(msg)
  }

  def acceptInboundMessage(msg: Any): Boolean = {
    iClass.isInstance(msg)
  }

  override def write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise): Unit = {
    if (acceptOutboundMessage(msg)) {
      super.write(ctx, msg, promise)
    } else {
      promise.setFailure(new IllegalArgumentException(s"msg is not a [${oClass.getName}]."))
    }
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    if (acceptInboundMessage(msg)) {
      super.channelRead(ctx, msg)
    } else {
      throw new IllegalArgumentException(s"msg is not a [${iClass.getName}].")
    }
  }
}

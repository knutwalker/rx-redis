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

package rx.redis.pipeline

import rx.redis.serialization.Deserializer

import io.netty.buffer.ByteBuf
import io.netty.channel.{ ChannelDuplexHandler, ChannelHandlerContext, ChannelPromise }
import io.netty.util.ReferenceCountUtil

private[redis] class RespCodec extends ChannelDuplexHandler with RespDecoder {
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    if (Deserializer.RespFailure(cause)) {
      // TODO: error logging
      println("cause = " + cause)
    } else {
      super.exceptionCaught(ctx, cause)
    }
  }
  override def write(ctx: ChannelHandlerContext, msg: scala.Any, promise: ChannelPromise): Unit = {
    try {
      super.write(ctx, msg.asInstanceOf[ByteBuf], promise)
    } catch {
      case _: ClassCastException ⇒
        ReferenceCountUtil.release(msg)
        promise.setFailure(new IllegalArgumentException("msg is not a [io.netty.buffer.ByteBuf]."))
    }
  }
}

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

import io.netty.buffer.Unpooled
import io.netty.channel.{ ChannelPromise, ChannelHandlerContext, ChannelOutboundHandler }
import io.netty.util.ReferenceCountUtil

import rx.redis.resp.RespType
import rx.redis.serialization.ByteBufSerializer

private[redis] trait RespEncoder { this: ChannelOutboundHandler ⇒

  final override def write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise): Unit = msg match {
    case data: RespType ⇒
      encode(ctx, promise, data)
    case _ ⇒
      ReferenceCountUtil.release(msg)
      promise.setFailure(new IllegalArgumentException(s"msg is not a [${classOf[RespType].getName}]."))
  }

  private[this] final def encode(ctx: ChannelHandlerContext, promise: ChannelPromise, data: RespType): Unit = {
    val buf = ctx.alloc.ioBuffer
    try {
      ByteBufSerializer(data, buf)
    } catch {
      case e: Throwable ⇒
        buf.release()
        promise.setFailure(e)
    }
    if (buf.isReadable) {
      ctx.write(buf, promise)
    } else {
      buf.release()
      ctx.write(Unpooled.EMPTY_BUFFER, promise)
    }
  }

}

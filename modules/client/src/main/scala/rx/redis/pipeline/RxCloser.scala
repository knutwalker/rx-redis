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

import rx.Observer
import io.netty.channel.{ ChannelHandlerContext, ChannelPromise, ChannelDuplexHandler }
import io.netty.util.ReferenceCountUtil

import rx.redis.resp.RespType

import java.util
import scala.util.control.NoStackTrace

private[redis] class RxCloser(queue: util.Queue[Observer[RespType]]) extends ChannelDuplexHandler {

  private final var canWrite = true
  private final var closePromise: ChannelPromise = _
  private final val ChannelClosedException = new IllegalStateException("Channel already closed") with NoStackTrace

  private def canClose = !canWrite && (closePromise ne null)

  private def closeChannel(ctx: ChannelHandlerContext): Unit = {
    val promise = closePromise
    closePromise = null
    ctx.close(promise)
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    ReferenceCountUtil.release(msg)
    if (canClose && queue.isEmpty) {
      closeChannel(ctx)
    }
  }

  override def write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise): Unit = {
    if (canWrite) {
      ctx.write(msg, promise)
    } else {
      ReferenceCountUtil.release(msg)
      promise.setFailure(ChannelClosedException)
    }
  }

  override def close(ctx: ChannelHandlerContext, future: ChannelPromise): Unit = {
    canWrite = false
    if (queue.isEmpty) {
      ctx.close(future)
    } else {
      closePromise = future
    }
  }

}

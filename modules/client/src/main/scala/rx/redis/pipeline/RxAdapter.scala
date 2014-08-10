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
import io.netty.channel.{ ChannelDuplexHandler, ChannelHandlerContext, ChannelPromise }
import io.netty.util.ReferenceCountUtil
import io.netty.util.internal.PlatformDependent

import rx.redis.resp.RespType

import scala.util.control.NoStackTrace

private[redis] class RxAdapter extends ChannelDuplexHandler {

  private final val queue = PlatformDependent.newMpscQueue[Observer[RespType]]
  private final var canWrite = true
  private final var closePromise: ChannelPromise = _
  private final val ChannelClosedException = new IllegalStateException("Channel already closed") with NoStackTrace

  private def canClose: Boolean = !canWrite && (closePromise ne null)

  private def closeChannel(ctx: ChannelHandlerContext): Unit = {
    val promise = closePromise
    closePromise = null
    ctx.close(promise)
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    val sender = queue.poll()
    if (sender ne null) {
      try {
        sender.onNext(msg.asInstanceOf[RespType])
        sender.onCompleted()
      } catch {
        case cc: ClassCastException ⇒
          sender.onError(new RuntimeException("msg is not a RespType"))
      } finally {
        ReferenceCountUtil.release(msg)
      }
      if (queue.isEmpty && canClose) {
        closeChannel(ctx)
      }
    } else {
      ReferenceCountUtil.release(msg)
      if (canClose) {
        closeChannel(ctx)
      }
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = ()

  override def write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise): Unit = {
    if (canWrite) {
      try {
        val aa = msg.asInstanceOf[AdapterAction]
        if (aa.sender ne null) queue.offer(aa.sender)
        aa.action(ctx, aa.cmd, promise)
        aa.recycle()
      } catch {
        case cc: ClassCastException ⇒
          super.write(ctx, msg, promise)
      }
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

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

private[redis] class RxAdapter extends ChannelDuplexHandler {

  private final val queue = PlatformDependent.newMpscQueue[Observer[RespType]]

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
    } else {
      ReferenceCountUtil.release(msg)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = ()

  override def write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise): Unit = msg match {
    case aa @ AdapterAction(cmd, sender, action) ⇒
      queue.offer(sender)
      AdapterAction.recycle(aa)
      action(ctx, cmd, promise)
    case _ ⇒
      super.write(ctx, msg, promise)
  }
}

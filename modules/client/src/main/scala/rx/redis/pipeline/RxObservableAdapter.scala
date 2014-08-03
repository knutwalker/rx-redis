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
import io.netty.channel.{ ChannelHandlerAdapter, ChannelHandlerContext }
import io.netty.util.ReferenceCountUtil

class RxObservableAdapter[Recv <: AnyRef](responses: Observer[Recv]) extends ChannelHandlerAdapter {

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    try {
      responses.onNext(msg.asInstanceOf[Recv])
    } catch {
      case cc: ClassCastException ⇒
        responses.onError(new RuntimeException("msg is not a RespType"))
    } finally {
      ReferenceCountUtil.release(msg)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    responses.onError(cause)
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    responses.onCompleted()
  }
}

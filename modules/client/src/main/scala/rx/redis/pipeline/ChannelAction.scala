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

import io.netty.channel.{ ChannelPromise, ChannelHandlerContext }

import rx.redis.resp.DataType

sealed trait ChannelAction extends ((ChannelHandlerContext, DataType, ChannelPromise) ⇒ Unit)

object ChannelAction {

  object Write extends ChannelAction {
    def apply(ctx: ChannelHandlerContext, cmd: DataType, promise: ChannelPromise): Unit = {
      ctx.write(cmd, promise)
    }
    override def toString(): String = "ChannelAction[Write]"
  }

  object Flush extends ChannelAction {
    def apply(ctx: ChannelHandlerContext, cmd: DataType, promise: ChannelPromise): Unit = {
      try {
        ctx.flush()
        promise.setSuccess()
      } catch {
        case t: Throwable ⇒ promise.setFailure(t)
      }
    }
    override def toString(): String = "ChannelAction[Flush]"
  }

  object WriteAndFlush extends ChannelAction {
    def apply(ctx: ChannelHandlerContext, cmd: DataType, promise: ChannelPromise): Unit = {
      ctx.writeAndFlush(cmd, promise)
    }
    override def toString(): String = "ChannelAction[WriteAndFlush]"
  }
}


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

import rx.redis.resp.RespType

import rx.Observer

import io.netty.channel.{ ChannelDuplexHandler, ChannelHandlerContext, ChannelPromise }

import scala.util.control.NonFatal

import java.util

private[redis] class RxAdapter(queue: util.Queue[Observer[RespType]]) extends ChannelDuplexHandler {

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    val sender = queue.poll()
    if (sender ne null) {
      try {
        sender.onNext(msg.asInstanceOf[RespType])
        sender.onCompleted()
      } catch {
        case cc: ClassCastException ⇒
          sender.onError(new RuntimeException("msg is not a [rx.redis.resp.RespType]"))
        case NonFatal(ex) ⇒
          sender.onError(ex)
      }
    }
    ctx.fireChannelRead(msg)
  }

  override def write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise): Unit = {
    try {
      val aa = msg.asInstanceOf[AdapterAction]
      queue.offer(aa.sender) // TODO: offer may fail, check result
      ctx.write(aa.cmd, promise)
      aa.recycle()
    } catch {
      case cc: ClassCastException ⇒
        ctx.write(msg, promise)
    }
  }
}

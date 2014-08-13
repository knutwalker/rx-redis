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

import io.netty.buffer.{ ByteBuf, ByteBufAllocator }
import io.netty.channel.{ ChannelHandlerContext, ChannelInboundHandler }

import rx.redis.serialization.ByteBufDeserializer

private[redis] trait RespDecoder { this: ChannelInboundHandler ⇒

  private final var buffered: ByteBuf = null

  final override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = msg match {
    case in: ByteBuf ⇒
      decode(ctx, in)
    case _ ⇒
      throw new IllegalArgumentException(s"msg is not a [${classOf[ByteBuf].getName}].")
  }

  final override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    if (buffered ne null) {
      buffered.discardSomeReadBytes()
    }
    ctx.fireChannelReadComplete()
  }

  final override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    if (buffered ne null) {
      decode0(ctx, buffered)
      buffered.release()
      buffered = null
    }
    ctx.fireChannelInactive()
  }

  private final def decode(ctx: ChannelHandlerContext, data: ByteBuf): Unit = {
    val completeData = mergeFrames(ctx.alloc(), data)
    decode0(ctx, completeData)
  }

  private final def decode0(ctx: ChannelHandlerContext, completeData: ByteBuf): Unit = {
    val needMore = ByteBufDeserializer.foreach(completeData) { resp ⇒
      ctx.fireChannelRead(resp)
    }
    if (needMore.isDefined) {
      buffered = completeData
    } else {
      completeData.release()
    }
  }

  private final def mergeFrames(alloc: ByteBufAllocator, frame: ByteBuf): ByteBuf = {
    if (buffered eq null) {
      frame
    } else {
      val buf = ensureSize(alloc, frame.readableBytes())
      buf.writeBytes(frame)
      frame.release()
      buf
    }
  }

  private final def ensureSize(alloc: ByteBufAllocator, size: Int): ByteBuf = {
    var newBuf = buffered
    if (newBuf.writerIndex > newBuf.maxCapacity - size) {
      val buf = alloc.buffer(newBuf.readableBytes + size)
      buf.writeBytes(newBuf)
      newBuf.release()
      newBuf = buf
    }
    buffered = null
    newBuf
  }
}

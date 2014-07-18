package rx.redis
package protocol

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}

import rx.redis.resp.Parser


private[redis] final class Decoder extends ChannelInboundHandlerAdapter {

  private var incompleteBit: Option[ByteBuf] = None

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = msg match {
    case bb: ByteBuf => decode(ctx, bb)
    case _ => super.channelRead(ctx, msg)
  }

  private def completeDataFrame(bb: ByteBuf): ByteBuf =
    incompleteBit.fold(bb) { older =>
      incompleteBit = None
      Unpooled.copiedBuffer(older, bb)
    }

  private def decode(ctx: ChannelHandlerContext, bb: ByteBuf): Unit = {
    val data = completeDataFrame(bb)
    Parser.foreach(data) { res =>
      super.channelRead(ctx, res)
    }.foreach(ned => incompleteBit = Some(ned.remaining))
  }
}

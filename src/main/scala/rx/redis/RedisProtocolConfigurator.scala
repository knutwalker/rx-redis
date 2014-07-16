package rx.redis

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelPipeline}
import io.reactivex.netty.pipeline.PipelineConfigurator
import rx.redis.resp.{RespType, Parser}

final class RedisProtocolConfigurator extends PipelineConfigurator[RespType, ByteBuf] {
  import rx.redis.RedisProtocolConfigurator.RedisDecoder
  def configureNewPipeline(pipeline: ChannelPipeline): Unit = {
    pipeline.addLast(new RedisDecoder)
  }
}

object RedisProtocolConfigurator {
  private final class RedisDecoder extends ChannelInboundHandlerAdapter {

    private var incompleteBit: Option[ByteBuf] = None

    override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit =
      msg match {
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
}

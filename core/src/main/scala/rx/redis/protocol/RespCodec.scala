package rx.redis.protocol

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec

import rx.redis.resp.DataType

import java.util

@Sharable
class RespCodec extends ByteToMessageCodec[DataType] {
  def encode(ctx: ChannelHandlerContext, msg: DataType, out: ByteBuf): Unit = {
    Serializer(msg, out)
  }

  def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    Deserializer.foreach(in)(out.add(_))
  }
}

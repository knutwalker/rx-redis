package rx.redis.protocol

import io.netty.channel.ChannelPipeline
import io.reactivex.netty.pipeline.PipelineConfigurator

import rx.redis.resp.{DataType, RespType}


private[redis] final class Configurator extends PipelineConfigurator[RespType, DataType] {
  def configureNewPipeline(pipeline: ChannelPipeline): Unit = {
    pipeline.addLast(new RespCodec)
  }
}

package rx.redis

import io.reactivex.netty.RxNetty

import rx.redis.client.RxRedisClient
import rx.redis.pipeline.Configurator

object RxRedis {
  def apply(host: String, port: Int): api.Client = {
    val builder =
      RxNetty.newTcpClientBuilder(host, port)
        .defaultTcpOptions()
        .withName("Redis")
        .pipelineConfigurator(new Configurator)
    new RxRedisClient(builder.build())
  }

  def connect(host: String, port: Int): api.Client = apply(host, port)

  def await(client: api.Client): Unit = {
    client.closedObservable.toBlocking.lastOrDefault(())
  }
}

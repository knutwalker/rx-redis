package rx.redis

import io.reactivex.netty.RxNetty

import rx.redis.client.{DefaultClient, ThreadSafeClient}
import rx.redis.protocol.Configurator


object RxRedis {
  def apply(host: String, port: Int, shareable: Boolean = true): api.Client = {
    val builder =
      RxNetty.newTcpClientBuilder(host, port)
        .defaultTcpOptions()
        .withName("Redis")
        .pipelineConfigurator(new Configurator)
    val client = new DefaultClient(builder.build())
    if (shareable) new ThreadSafeClient(client) else client
  }

  def connect(host: String, port: Int): api.Client = apply(host, port)

  def await(client: api.Client): Unit = {
    client.closedObservable.toBlocking.lastOrDefault(())
  }
}

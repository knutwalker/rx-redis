package rx.redis

import io.reactivex.netty.RxNetty

import rx.redis.client.{DefaultClient, ThreadSafeClient}
import rx.redis.protocol.Configurator


object RxRedis {
  def apply(host: String, port: Int, shareable: Boolean = true): api.Client = {
    val client = new DefaultClient(RxNetty.createTcpClient(host, port, new Configurator))
    if (shareable) new ThreadSafeClient(client) else client
  }

  def connect(host: String, port: Int): api.Client = apply(host, port)
  def connect(host: String, port: Int, shareable: Boolean): api.Client = apply(host, port, shareable)

  def await(client: api.Client): Unit = {
    client.closedObservable.toBlocking.lastOrDefault(())
  }
}

package rx.redis.api

import rx.redis.client.RawClient


object RxRedis {
  def apply(host: String, port: Int, shareable: Boolean = true): Client = {
    new Client(RawClient(host, port, shareable))
  }

  def await(client: Client): Unit = {
    client.closedObservable.toBlocking.toList.lastOption.getOrElse(())
  }
}

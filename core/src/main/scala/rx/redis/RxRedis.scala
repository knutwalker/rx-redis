package rx.redis

import rx.redis.client.RawClient


object RxRedis {
  def apply(host: String, port: Int, shareable: Boolean = true): api.Client = {
    new api.Client(RawClient(host, port, shareable))
  }

  def await(client: api.Client): Unit = {
    client.closedObservable.toBlocking.toList.lastOption.getOrElse(())
  }
}

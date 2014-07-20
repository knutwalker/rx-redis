package rx.redis.client

import rx.Observable

import rx.redis.api
import rx.redis.resp.{DataType, RespType}
import rx.redis.serialization.Writes


private[redis] final class ThreadSafeClient(underlying: api.Client)
  extends api.Client
  with Commands {


  def command(cmd: DataType): Observable[RespType] = synchronized {
    underlying.command(cmd)
  }

  def command[A: Writes](cmd: A): Observable[RespType] = synchronized {
    underlying.command(cmd)
  }

  def closedObservable: Observable[Unit] =
    underlying.closedObservable

  def shutdown(): Observable[Unit] =
    underlying.shutdown()
}

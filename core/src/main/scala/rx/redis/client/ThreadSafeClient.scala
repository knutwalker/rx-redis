package rx.redis.client

import rx.Observable

import rx.redis.resp.{DataType, RespType}


private[redis] final class ThreadSafeClient(underlying: RawClient)
    extends RawClient {

  def command(cmd: DataType): Observable[RespType] = synchronized {
    underlying.command(cmd)
  }

//  def command[A: Writes](cmd: A): Observable[RespType] = synchronized {
//    underlying.command(cmd)
//  }

  def closedObservable: Observable[Unit] =
    underlying.closedObservable

  def shutdown(): Observable[Unit] =
    underlying.shutdown()
}

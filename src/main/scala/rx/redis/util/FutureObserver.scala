package rx.redis.util

import rx.lang.scala.Observer

import scala.concurrent.Promise


class FutureObserver[T](p: Promise[Unit]) extends Observer[T] {
  override def onCompleted(): Unit = p.success(())
  override def onError(error: Throwable): Unit = p.failure(error)
}

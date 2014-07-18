package rx.redis.util.observers

import rx.lang.scala.{Observable, Observer}

import scala.concurrent.{Future, Promise}


object AsFutureObserver {
  def apply[A](target: Observable[A]): Future[Unit] = {
    val p = Promise[Unit]()
    target.subscribe(new AsFutureObserver[A](p))
    p.future
  }
}

final class AsFutureObserver[A](p: Promise[Unit]) extends Observer[A] {
  override def onCompleted(): Unit = p.success(())
  override def onError(error: Throwable): Unit = p.failure(error)
}

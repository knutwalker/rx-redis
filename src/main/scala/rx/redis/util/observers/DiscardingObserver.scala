package rx.redis.util.observers

import rx.subjects.AsyncSubject
import rx.{Observer, Observable}


object DiscardingObserver {
  def apply[A](o: Observable[A]): Observable[Unit] = {
    val s = AsyncSubject.create[Unit]()
    o.subscribe(new DiscardingObserver[A](s))
    s
  }
}

final class DiscardingObserver[A](target: Observer[_]) extends Observer[A] {
  def onNext(t: A): Unit = ()
  def onError(error: Throwable): Unit = target.onError(error)
  def onCompleted(): Unit = target.onCompleted()
}

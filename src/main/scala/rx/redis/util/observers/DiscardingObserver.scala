package rx.redis.util.observers

import rx.lang.scala.subjects.AsyncSubject
import rx.lang.scala.{Observable, Observer}


object DiscardingObserver {
  def apply[A](o: Observable[A]): Observable[Unit] = {
    val s = AsyncSubject[Unit]()
    o.subscribe(new DiscardingObserver[A](s))
    s
  }
}
class DiscardingObserver[A](target: Observer[_]) extends Observer[A] {
  override def onError(error: Throwable): Unit = target.onError(error)
  override def onCompleted(): Unit = target.onCompleted()
}

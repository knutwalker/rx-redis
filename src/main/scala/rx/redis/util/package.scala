package rx.redis

import rx.lang.scala.Observable

import scala.concurrent.{Future, Promise}

// all temp in here
package object util {
  val command = (s: String) => {
    val sb = new StringBuilder

    val items = s.split(' ')

    sb += '*'
    sb ++= items.size.toString
    sb ++= "\r\n"

    for (item <- items) {
    sb += '$'
    sb ++= item.length.toString
    sb ++= "\r\n"
    sb ++= item
    sb ++= "\r\n"
    }

    sb.result()
  }

  def observeAsFuture[T](o: Observable[T]): Future[Unit] = {
    val p = Promise[Unit]()
    o.subscribe(new FutureObserver[T](p))
    p.future
  }
}

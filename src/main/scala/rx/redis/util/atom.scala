package rx.redis.util

import scala.annotation.tailrec
import java.util.concurrent.atomic.AtomicReference

object atom {

  type Atom[T] = AtomicReference[T]

  implicit final class RichAtom[T](val underlying: AtomicReference[T]) extends AnyVal {
    def `@`: T = underlying.get()
    def reset(v: T) = underlying.set(v)
    def swap(fun: T => T): Unit = {
      @tailrec def loop(): Boolean = {
        val prev = underlying.get()
        val next = fun(prev)
        underlying.compareAndSet(prev, next) || loop()
      }
      loop()
    }
    def swapAndReturn[R](fun: T => (T, R)): R = {
      @tailrec def loop(): R = {
        val prev = underlying.get()
        val (next, value) = fun(prev)
        val success = underlying.compareAndSet(prev, next)
        if (success) value else loop()
      }
      loop()
    }
    def foreach(fun: T => Unit) = fun(underlying.get())
  }

  def Atom[T](item: T): Atom[T] = new AtomicReference(item)
}

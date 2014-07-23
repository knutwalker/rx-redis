package rx.lang.scala


object XJavaConversions {
  import language.implicitConversions

  implicit def toJavaBlockingObservable[T](b: observables.BlockingObservable[T]): rx.observables.BlockingObservable[_ <: T] = {
    b.asJava
  }

  implicit def toScalaBlockingObservable[T](b: rx.observables.BlockingObservable[_ <: T]): observables.BlockingObservable[T] = {
    new observables.BlockingObservable[T](b)
  }
}

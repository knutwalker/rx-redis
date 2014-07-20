package rx.redis.serialization

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import scala.annotation.{implicitNotFound, tailrec}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.{Deadline, FiniteDuration}


@implicitNotFound("No type class found for ${A}. You have to implement an rx.redis.serialization.Bytes[${A}] in order to use ${A} as custom value.")
trait Bytes[A] {

  def bytes(value: A): Array[Byte]
}

object Bytes {

  @inline def apply[T](implicit T: Bytes[T]): Bytes[T] = T

  private final val long2bytes: (Long => Array[Byte]) = (n) => {
    @tailrec
    def loop(l: Long, b: ArrayBuffer[Byte]): Array[Byte] = {
      if (l == 0) b.reverse.toArray
      else {
        b += (l % 10 + '0').toByte
        loop(l / 10, b)
      }
    }
    if (n == 0) Array((0 + '0').toByte)
    else loop(n, new ArrayBuffer[Byte](10))
  }

  abstract class CompositeBytes[A, B: Bytes] extends Bytes[A] {
    final def bytes(value: A): Array[Byte] = {
      val composed = compose(value)
      Bytes[B].bytes(composed)
    }
    def compose(value: A): B
  }
  object CompositeBytes {
    def apply[A, B: Bytes](f: A => B): Bytes[A] =
      new CompositeBytes[A, B] {
        def compose(value: A): B = f(value)
      }
  }

  implicit object StringBytes extends Bytes[String] {
    private final val charset = Charset.defaultCharset

    def bytes(value: String): Array[Byte] =
      value.getBytes(charset)
  }

  implicit object ByteArrayBytes extends Bytes[Array[Byte]] {
    def bytes(value: Array[Byte]): Array[Byte] = value
  }

  implicit def SeqBytes[A: Bytes]: Bytes[Seq[A]] = new Bytes[Seq[A]] {
    def bytes(value: Seq[A]): Array[Byte] = {
      val baos = new ByteArrayOutputStream()
      val tci = Bytes[A]
      for (v <- value) {
        baos.write(tci.bytes(v))
      }
      baos.close()
      baos.toByteArray
    }
  }

  implicit val LongBytes = CompositeBytes(long2bytes)

  implicit val DurationBytes = CompositeBytes((_: FiniteDuration).toSeconds)

  implicit val DeadlineBytes = CompositeBytes { d: Deadline =>
    (d.timeLeft.toMillis + System.currentTimeMillis()) / 1000
  }
}

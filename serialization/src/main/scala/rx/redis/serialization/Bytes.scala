package rx.redis.serialization

import io.netty.buffer.{ByteBuf, ByteBufAllocator}
import io.reactivex.netty.channel.ContentTransformer

import java.nio.charset.Charset
import scala.annotation.implicitNotFound
import scala.concurrent.duration.{Deadline, FiniteDuration}


@implicitNotFound("No type class found for ${A}. You have to implement an rx.redis.serialization.Bytes[${A}] in order to use ${A} as custom value.")
trait Bytes[A] {

  def write(value: A, allocator: ByteBufAllocator): ByteBuf

  lazy val contentTransformer: ContentTransformer[A] =
    new ContentTransformer[A] {
      def call(t1: A, t2: ByteBufAllocator): ByteBuf = write(t1, t2)
    }
}

object Bytes {

  @inline def apply[T](implicit T: Bytes[T]): Bytes[T] = T

  abstract class CompositeBytes[A, B: Bytes] extends Bytes[A] {
    final def write(value: A, allocator: ByteBufAllocator): ByteBuf = {
      val composed = compose(value)
      Bytes[B].write(composed, allocator)
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

    def write(value: String, allocator: ByteBufAllocator): ByteBuf = {
      val contentAsBytes: Array[Byte] = value.getBytes(charset)
      allocator.buffer(contentAsBytes.length).writeBytes(contentAsBytes)
    }
  }

  implicit object ByteArrayBytes extends Bytes[Array[Byte]] {
    def write(value: Array[Byte], allocator: ByteBufAllocator): ByteBuf = {
      allocator.buffer(value.length).writeBytes(value)
    }
  }

  implicit object ByteBufBytes extends Bytes[ByteBuf] {
    def write(value: ByteBuf, allocator: ByteBufAllocator): ByteBuf = value
  }

  implicit def SeqBytes[A: Bytes]: Bytes[Seq[A]] = new Bytes[Seq[A]] {
    def write(value: Seq[A], allocator: ByteBufAllocator): ByteBuf = {
      val buf = allocator.buffer()
      val tci = Bytes[A]
      for (v <- value) {
        buf.writeBytes(tci.write(v, buf.alloc()))
      }
      buf
    }
  }

  implicit val LongBytes = CompositeBytes(Writes.long2bytes(_: Long))

  implicit val DurationBytes = CompositeBytes((_: FiniteDuration).toSeconds)

  implicit val DeadlineBytes = CompositeBytes { d: Deadline =>
    (d.timeLeft.toMillis + System.currentTimeMillis()) / 1000
  }
}

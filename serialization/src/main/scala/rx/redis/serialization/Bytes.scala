package rx.redis.serialization

import io.netty.buffer.{ByteBuf, ByteBufAllocator}
import io.reactivex.netty.channel.ContentTransformer

import java.nio.charset.Charset
import scala.annotation.implicitNotFound
import scala.concurrent.duration.Duration


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

  implicit object LongBytes extends Bytes[Long] {
    def write(value: Long, allocator: ByteBufAllocator): ByteBuf = {
      ByteArrayBytes.write(Writes.long2bytes(value), allocator)
    }
  }

  implicit object DurationBytes extends Bytes[Duration] {
    def write(value: Duration, allocator: ByteBufAllocator): ByteBuf = {
      LongBytes.write(value.toSeconds, allocator)
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
}

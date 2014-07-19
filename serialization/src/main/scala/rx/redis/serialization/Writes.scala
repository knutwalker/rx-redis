package rx.redis.serialization

import io.netty.buffer.{ByteBuf, ByteBufAllocator}
import io.reactivex.netty.channel.ContentTransformer

import java.nio.charset.Charset
import scala.annotation.{implicitNotFound, tailrec}
import scala.collection.mutable.ArrayBuffer
import scala.language.experimental.macros


@implicitNotFound("No type class found for ${A}. You have to implement an rx.redis.api.Write[${A}] in order to send ${A} directly.")
trait Writes[A] {

  def write(value: A, allocator: ByteBufAllocator): ByteBuf

  lazy val contentTransformer: ContentTransformer[A] =
    new ContentTransformer[A] {
      def call(t1: A, t2: ByteBufAllocator): ByteBuf = write(t1, t2)
    }

  protected def int2bytes(n: Int): Array[Byte] = {
    @tailrec
    def loop(l: Int, b: ArrayBuffer[Byte]): Array[Byte] = {
      if (l == 0) b.reverse.toArray
      else {
        b += (l % 10 + '0').toByte
        loop(l / 10, b)
      }
    }
    loop(n, new ArrayBuffer[Byte](10))
  }
}

object Writes {

  @inline def apply[T](implicit T: Writes[T]): Writes[T] = T

  def writes[A]: Writes[A] = macro Macros.writes[A]

  implicit object DefaultStringWrites extends Writes[String] {
    private final val charset = Charset.defaultCharset

    def write(value: String, allocator: ByteBufAllocator): ByteBuf = {
      val contentAsBytes: Array[Byte] = value.getBytes(charset)
      allocator.buffer(contentAsBytes.length).writeBytes(contentAsBytes)
    }
  }

  implicit object ByteArrayWrites extends Writes[Array[Byte]] {
    def write(value: Array[Byte], allocator: ByteBufAllocator): ByteBuf = {
      allocator.buffer(value.length).writeBytes(value)
    }
  }
}

package rx.redis.api

import io.netty.buffer.{ByteBuf, ByteBufAllocator, PooledByteBufAllocator, UnpooledByteBufAllocator}
import io.reactivex.netty.channel.ContentTransformer

import java.nio.charset.Charset
import scala.annotation.implicitNotFound


@implicitNotFound("No type class found for ${A}. You have to implement an rx.redis.api.Write[${A}] in order to send ${A} directly.")
trait Writes[A] {

  def toBytes(value: A, allocator: ByteBufAllocator): ByteBuf

  def toBytes(value: A): ByteBuf =
    toBytes(value, Writes.unpooled)

  lazy val contentTransformer: ContentTransformer[A] =
    new ContentTransformer[A] {
      def call(t1: A, t2: ByteBufAllocator): ByteBuf = toBytes(t1, t2)
    }
}

object Writes {
  private[redis] final val unpooled = UnpooledByteBufAllocator.DEFAULT
  private[redis] final val pooled = PooledByteBufAllocator.DEFAULT

  @inline def apply[T](implicit T: Writes[T]) = T

  implicit object DefaultStringWrites extends Writes[String] {
    private final val charset = Charset.defaultCharset

    def toBytes(value: String, allocator: ByteBufAllocator): ByteBuf = {
      val contentAsBytes: Array[Byte] = value.getBytes(charset)
      allocator.buffer(contentAsBytes.length).writeBytes(contentAsBytes)
    }
  }

  implicit object ByteArrayWrites extends Writes[Array[Byte]] {
    def toBytes(value: Array[Byte], allocator: ByteBufAllocator): ByteBuf = {
      allocator.buffer(value.length).writeBytes(value)
    }
  }
}

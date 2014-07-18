package rx.redis.api

import java.nio.charset.Charset

import scala.annotation.implicitNotFound

import io.netty.buffer.{ByteBuf, ByteBufAllocator, PooledByteBufAllocator, UnpooledByteBufAllocator}
import io.reactivex.netty.channel.ContentTransformer


@implicitNotFound("You have to implement the type class rx.redis.api.Write[$T] in order to send $T directly.")
trait Write[A] {

  def toBytes(value: A, allocator: ByteBufAllocator): ByteBuf

  def toBytes(value: A): ByteBuf =
    toBytes(value, Write.unpooled)

  lazy val contentTransformer: ContentTransformer[A] =
    new ContentTransformer[A] {
      def call(t1: A, t2: ByteBufAllocator): ByteBuf = toBytes(t1, t2)
    }
}

object Write {
  final val unpooled = UnpooledByteBufAllocator.DEFAULT
  final val pooled = PooledByteBufAllocator.DEFAULT

  @inline def apply[T](implicit T: Write[T]) = T

  implicit object DefaultStringWrite extends Write[String] {
    private final val charset = Charset.defaultCharset

    def toBytes(value: String, allocator: ByteBufAllocator): ByteBuf = {
      val contentAsBytes: Array[Byte] = value.getBytes(charset)
      allocator.buffer(contentAsBytes.length).writeBytes(contentAsBytes)
    }
  }

  implicit object ByteArrayWrite extends Write[Array[Byte]] {
    def toBytes(value: Array[Byte], allocator: ByteBufAllocator): ByteBuf = {
      allocator.buffer(value.length).writeBytes(value)
    }
  }
}

package rx.redis.commands

import java.nio.charset.Charset

import io.netty.buffer.{ByteBuf, ByteBufAllocator}

import rx.redis.api.Writes


case class Get(key: String)
object Get {
  implicit object GetWrites extends Writes[Get] {
    private final val charset = Charset.defaultCharset

    def toBytes(value: Get, allocator: ByteBufAllocator): ByteBuf =
      toBytes(value.key, allocator)

    def toBytes(key: String, allocator: ByteBufAllocator): ByteBuf = {
      val keyAsBytes = key.getBytes(charset)
      val keyLen = keyAsBytes.length
      val buf = allocator.buffer(18 + keyLen + (keyLen / 10))

      val prefix = "*2\r\n$3\r\nGET\r\n$".getBytes(charset)
      val suffix = "\r\n".getBytes(charset)

      buf.writeBytes(prefix)
        .writeBytes(keyLen.toString.getBytes)
        .writeBytes(suffix)
        .writeBytes(keyAsBytes)
        .writeBytes(suffix)
    }
  }
}


case class Set(key: String, value: ByteBuf)
object Set {
  def apply[A: Writes](key: String, value: A): Set =
    new Set(key, Writes[A].toBytes(value))

  implicit object SetWrites extends Writes[Set] {
    private final val charset = Charset.defaultCharset

    def toBytes(value: Set, allocator: ByteBufAllocator): ByteBuf =
      toBytes(value.key, value.value, allocator)

    def toBytes(key: String, value: ByteBuf, allocator: ByteBufAllocator): ByteBuf = {
      val keyAsBytes = key.getBytes(charset)
      val keyLen = keyAsBytes.length
      val valueLen = value.readableBytes()
      val buf = allocator.buffer(23 + keyLen + (keyLen / 10) + valueLen + (valueLen / 10))

      val prefix = "*3\r\n$3\r\nSET\r\n$".getBytes(charset)
      val suffix = "\r\n".getBytes(charset)

      buf.writeBytes(prefix)
        .writeBytes(keyLen.toString.getBytes)
        .writeBytes(suffix)
        .writeBytes(keyAsBytes)
        .writeBytes(suffix)
        .writeByte('$'.toByte)
        .writeBytes(valueLen.toString.getBytes)
        .writeBytes(suffix)
        .writeBytes(value)
        .writeBytes(suffix)
    }
  }
}

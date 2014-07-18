package rx.redis.japi

import io.netty.buffer.{ByteBufAllocator, ByteBuf}

import rx.redis.api.Writes


object DefaultWrites {
  private object ByteBufWrites extends Writes[ByteBuf] {
    def toBytes(value: ByteBuf, allocator: ByteBufAllocator): ByteBuf = value
  }

  val String: Writes[String] = Writes.DefaultStringWrites
  val ByteArray: Writes[Array[Byte]] = Writes.ByteArrayWrites
  val ByteBuf: Writes[ByteBuf] = ByteBufWrites
}

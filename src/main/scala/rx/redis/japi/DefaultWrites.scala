package rx.redis.japi

import io.netty.buffer.{ByteBuf, ByteBufAllocator}

import rx.redis.serialization.Writes


object DefaultWrites {
  private object ByteBufWrites extends Writes[ByteBuf] {
    def write(value: ByteBuf, allocator: ByteBufAllocator): ByteBuf = value
  }

  val String: Writes[String] = Writes.DefaultStringWrites
  val ByteArray: Writes[Array[Byte]] = Writes.ByteArrayWrites
  val ByteBuf: Writes[ByteBuf] = ByteBufWrites
}

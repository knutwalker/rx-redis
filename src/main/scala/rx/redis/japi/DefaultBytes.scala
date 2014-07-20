package rx.redis.japi

import io.netty.buffer.ByteBuf

import rx.redis.serialization.Bytes

import scala.concurrent.duration.Duration


object DefaultBytes {
  val String: Bytes[String] = Bytes.StringBytes
  val ByteArray: Bytes[Array[Byte]] = Bytes.ByteArrayBytes
  val ByteBuf: Bytes[ByteBuf] = Bytes.ByteBufBytes
  val Long: Bytes[Long] = Bytes.LongBytes
  val Duration: Bytes[Duration] = Bytes.DurationBytes
}

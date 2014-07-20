package rx.redis.japi

import rx.redis.serialization.Bytes
import rx.redis.serialization.Bytes.CompositeBytes

import scala.concurrent.duration.{Deadline, FiniteDuration}


object DefaultBytes {
  val String: Bytes[String] = Bytes.StringBytes
  val ByteArray: Bytes[Array[Byte]] = Bytes.ByteArrayBytes
  val Long: Bytes[java.lang.Long] = CompositeBytes((l: java.lang.Long) => l.longValue())
  val Duration: Bytes[FiniteDuration] = Bytes.DurationBytes
  val Deadline: Bytes[Deadline] = Bytes.DeadlineBytes
}

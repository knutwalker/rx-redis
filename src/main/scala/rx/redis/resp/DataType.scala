package rx.redis.resp

import io.netty.buffer.{ByteBuf, Unpooled}

import java.nio.charset.Charset


sealed abstract class RespType

sealed abstract class DataType extends RespType
case class RespString(data: String) extends DataType
case class RespError(reason: String) extends DataType
case class RespInteger(value: Long) extends DataType
case class RespArray(elements: List[DataType]) extends DataType
case object NullString extends DataType
case object NullArray extends DataType
case class RespBytes(bytes: ByteBuf) extends DataType {
  override def equals(obj: scala.Any): Boolean = obj match {
    case RespBytes(buf) => bytes.compareTo(buf) == 0
    case _ => super.equals(obj)
  }
}
object RespBytes {
  def apply(s: String, charset: Charset): RespBytes =
    apply(Unpooled.copiedBuffer(s, charset))

  def apply(s: String): RespBytes =
    apply(s, Charset.defaultCharset)
}

sealed abstract class ErrorType extends RespType
case class NotEnoughData(remaining: ByteBuf) extends ErrorType
case class ProtocolError(data: ByteBuf, expected: List[Byte]) extends ErrorType

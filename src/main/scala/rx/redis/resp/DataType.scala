package rx.redis.resp

import io.netty.buffer.{ByteBuf, ByteBufUtil, Unpooled}

import java.nio.charset.Charset


sealed abstract class RespType

sealed abstract class DataType extends RespType

case class RespString(data: String) extends DataType {
  override def toString: String = data
}

case class RespError(reason: String) extends DataType {
  override def toString: String = reason
}

case class RespInteger(value: Long) extends DataType {
  override def toString: String = value.toString
}

case class RespArray(elements: List[DataType]) extends DataType {
  override def toString: String = elements.map(_.toString).mkString("[", ", ", "]")
}

case class RespBytes(bytes: ByteBuf) extends DataType {
  override def equals(obj: scala.Any): Boolean = obj match {
    case RespBytes(buf) => ByteBufUtil.equals(bytes, buf)
    case _ => super.equals(obj)
  }

  override def toString: String = bytes.toString(Charset.defaultCharset())

  def toString(charset: Charset): String = bytes.toString(charset)
}
object RespBytes {
  def apply(s: String, charset: Charset): RespBytes =
    apply(Unpooled.copiedBuffer(s, charset))

  def apply(s: String): RespBytes =
    apply(s, Charset.defaultCharset)
}

case object NullString extends DataType {
  override def toString: String = "NULL"
}

case object NullArray extends DataType {
  override def toString: String = "NULL"
}


sealed abstract class ErrorType extends RespType

case class NotEnoughData(remaining: ByteBuf) extends ErrorType {
  override def toString: String = "[INCOMPLETE]: " + ByteBufUtil.hexDump(remaining)
}
case class ProtocolError(data: ByteBuf, expected: List[Byte]) extends ErrorType {
  override def toString: String = {
    val e = expected mkString ", "
    val pos = data.readerIndex()
    s"Protocol error at char $pos, expected [$e], but found [${data.getByte(pos).toChar}}]"
  }
}

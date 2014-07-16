package rx.redis.resp

import io.netty.buffer.ByteBuf

sealed abstract class RespType

sealed abstract class DataType extends RespType
case class RespString(data: String) extends DataType
case class RespError(reason: String) extends DataType
case class RespInteger(value: Long) extends DataType
case class RespArray(elements: List[DataType]) extends DataType
case object NullString extends DataType
case object NullArray extends DataType

sealed abstract class ErrorType extends RespType
case class NotEnoughData(remaining: ByteBuf) extends ErrorType
case class ProtocolError(data: ByteBuf, expected: List[Byte]) extends ErrorType

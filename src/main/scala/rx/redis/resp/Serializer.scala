package rx.redis.resp


import io.netty.buffer.{ByteBuf, ByteBufAllocator}

import rx.redis.serialization.Bytes

import java.nio.charset.Charset


object Serializer {

  private def INSTANCE = new Serializer()

  def apply(dt: DataType, bb: ByteBuf): ByteBuf = INSTANCE(dt, bb)

  def apply(dt: DataType, alloc: ByteBufAllocator): ByteBuf = {
    INSTANCE(dt, alloc.buffer())
  }
  def apply(dt: DataType, charset: Charset, alloc: ByteBufAllocator): String = {
    apply(dt, alloc).toString(charset)
  }
}

final class Serializer private () {
  import rx.redis.resp.Protocol._

  private def writeSimpleString(bb: ByteBuf, data: String): Unit = {
    val content = Bytes[String].bytes(data)
    bb.writeByte(Plus).writeBytes(content).writeBytes(CrLf)
  }

  private def writeError(bb: ByteBuf, data: String): Unit = {
    val content = Bytes[String].bytes(data)
    bb.writeByte(Minus).writeBytes(content).writeBytes(CrLf)
  }

  private def writeInteger(bb: ByteBuf, data: Long): Unit = {
    val content = Bytes[Long].bytes(data)
    bb.writeByte(Colon).writeBytes(content).writeBytes(CrLf)
  }

  private def writeArray(bb: ByteBuf, items: Array[DataType]): Unit = {
    val size = Bytes[Long].bytes(items.length)
    bb.writeByte(Asterisk).
      writeBytes(size).
      writeBytes(CrLf)
    items.foreach(item => quickApply(item, bb))
  }

  private def writeBytes(bb: ByteBuf, bytes: Array[Byte]): Unit = {
    bb.writeByte(Dollar).
      writeBytes(Bytes[Long].bytes(bytes.length)).
      writeBytes(CrLf).
      writeBytes(bytes).
      writeBytes(CrLf)
  }

  def writeNullString(bb: ByteBuf): Unit = {
    bb.writeByte(Dollar).writeBytes(Nullary).writeBytes(CrLf)
  }

  def writeNullArray(bb: ByteBuf): Unit = {
    bb.writeByte(Asterisk).writeBytes(Nullary).writeBytes(CrLf)
  }
  private def quickApply(data: DataType, bb: ByteBuf): Unit = data match {
    case RespString(s) => writeSimpleString(bb, s)
    case RespError(e) => writeError(bb, e)
    case RespInteger(l) => writeInteger(bb, l)
    case RespArray(ds) => writeArray(bb, ds)
    case RespBytes(bs) => writeBytes(bb, bs)
    case NullString => writeNullString(bb)
    case NullArray => writeNullArray(bb)
  }

  def apply(data: DataType, bb: ByteBuf): ByteBuf = {
//    bb.markWriterIndex()
//    try quickApply(data, bb) catch {
//      case _: Throwable => bb.resetWriterIndex()
//    }
//    bb
    quickApply(data, bb)
    bb
  }
}

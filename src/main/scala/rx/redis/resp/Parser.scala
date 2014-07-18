package rx.redis.resp

import java.nio.charset.StandardCharsets

import scala.annotation.tailrec
import scala.collection.immutable
import scala.collection.mutable.ListBuffer

import io.netty.buffer.{ByteBuf, Unpooled}

object Parser {
  private final val Plus = '+'.toByte
  private final val Minus = '-'.toByte
  private final val Colon = ':'.toByte
  private final val Dollar = '$'.toByte
  private final val Asterisk = '*'.toByte

  private final  val typeChars = List(Plus, Minus, Colon, Dollar, Asterisk)
  
  private final val Cr = '\r'.toByte
  private final val Lf = '\n'.toByte

  final val Utf8 = StandardCharsets.UTF_8

  type ParserFn = () => RespType
  type RespHandler = RespType => Unit

  def apply(bb: ByteBuf): ParserFn = new Parser(bb)
  def apply(bytes: Array[Byte]): ParserFn = new Parser(Unpooled.copiedBuffer(bytes))
  def apply(string: String): ParserFn = new Parser(Unpooled.copiedBuffer(string, Utf8))

  def foreach(bb: ByteBuf)(f: RespHandler): Option[NotEnoughData] = {
    val parser = Parser(bb)
    @tailrec def loop(): Option[NotEnoughData] =
      if (!bb.isReadable) None
      else parser() match {
        case ned: NotEnoughData => Some(ned)
        case x => f(x); loop()
      }
    loop()
  }
  def foreach(bytes: Array[Byte])(f: RespHandler): Option[NotEnoughData] = foreach(Unpooled.copiedBuffer(bytes))(f)
  def foreach(string: String)(f: RespHandler): Option[NotEnoughData] = foreach(Unpooled.copiedBuffer(string, Utf8))(f)

  def parseAll(bb: ByteBuf): immutable.Seq[RespType] = {
    val lb = new ListBuffer[RespType]()
    foreach(bb)(lb += _) foreach (lb += _)
    lb.result()
  }
  def parseAll(bytes: Array[Byte]): immutable.Seq[RespType] = parseAll(Unpooled.copiedBuffer(bytes))
  def parseAll(string: String): immutable.Seq[RespType] = parseAll(Unpooled.copiedBuffer(string, Utf8))
}

final class Parser private (bb: ByteBuf) extends Parser.ParserFn {
  import rx.redis.resp.Parser._

  @inline private def peek =
    bb.getByte(bb.readerIndex())

  @inline private def read(): Byte =
    bb.readByte()

  @inline private def read(b: Byte): Boolean = {
    val expected = peek == b
    if (expected) read()
    expected
  }

  @inline private def requireCrLf[T](ok: => RespType) = {
    if (!read(Cr)) ProtocolError(bb, List(Cr))
    else if (!read(Lf)) ProtocolError(bb, List(Lf))
    else ok
  }

  @inline private def requireLen(len: Int) =
    bb.isReadable(len)

  @inline private def notEnoughData() =
    NotEnoughData(bb.resetReaderIndex())

  @inline private def unknownType() =
    ProtocolError(bb.resetReaderIndex(), typeChars)

  @tailrec
  private def parseInt(n: Int, neg: Boolean): Int = {
    val current = read()
    current match {
      case Cr => read(); if (neg) -n else n
      case Minus => parseInt(n, neg = true)
      case b => parseInt(n * 10 + (b - '0'), neg)
    }
  }
  private def parseInt(): Int = parseInt(0, neg = false)

  @tailrec
  private def parseLong(n: Long, neg: Boolean): Long = {
    val current = read()
    current match {
      case Cr => read(); if (neg) -n else n
      case Minus => parseLong(n * -1, neg = true)
      case b => parseLong(n * 10 + (b - '0'), neg)
    }
  }
  private def parseLong(): Long = parseLong(0, neg = false)

  @inline private def parseLen() =
    parseInt()

  @inline private def parseInteger() =
    RespInteger(parseLong())

  private def readStringOfLen(len: Int)(ct: String => DataType) = {
    if (!requireLen(len)) notEnoughData()
    else {
      val slice = bb.readSlice(len)
      requireCrLf(ct(slice.toString(Utf8)))
    }
  }

  private def parseSimpleString() = {
    val len = bb.bytesBefore(Cr)
    if (len == -1) notEnoughData()
    else readStringOfLen(len)(RespString)
  }

  private def parseError() = {
    val len = bb.bytesBefore(Cr)
    if (len == -1) notEnoughData()
    else readStringOfLen(len)(RespError)
  }

  private def parseBulkString() = {
    val len = parseLen()
    if (len == -1) NullString
    else readStringOfLen(len)(RespString)
  }

  private def parseArray() = {
    val size = parseLen()
    if (size == -1) NullArray
    else {
      val lb = new ListBuffer[DataType]()
      @tailrec def loop(n: Int): RespType = {
        if (n == 0) RespArray(lb.result())
        else quickApply() match {
          case dt: DataType =>
            lb += dt
            loop(n - 1)
          case et: ErrorType => et
        }
      }
      loop(size)
    }
  }

  private def quickApply(): RespType = {
    if (!bb.isReadable(1)) notEnoughData()
    else {
      val firstByte = read()
      firstByte match {
        case Plus => parseSimpleString()
        case Minus => parseError()
        case Colon => parseInteger()
        case Dollar => parseBulkString()
        case Asterisk => parseArray()
        case _ => unknownType()
      }
    }
  }

  def apply(): RespType = {
    bb.markReaderIndex()
    quickApply()
  }
}

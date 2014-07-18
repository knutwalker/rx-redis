package rx.redis.resp

import io.netty.buffer.{ByteBuf, Unpooled}

import java.nio.charset.StandardCharsets
import scala.annotation.tailrec
import scala.collection.immutable
import scala.collection.mutable.ListBuffer


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

  private trait Num[@specialized(Int, Long) A] {
    def times(a: A, b: A): A
    def decShiftLeft(a: A, ones: Int): A
    def zero: A
    def one: A
    def minusOne: A
  }

  implicit private object IntIsNum extends Num[Int] {
    def times(a: Int, b: Int): Int = a * b
    def decShiftLeft(a: Int, ones: Int): Int = (a * 10) + ones
    val one: Int = 1
    val minusOne: Int = -1
    val zero: Int = 0
  }

  implicit private object LongIsNum extends Num[Long] {
    def times(a: Long, b: Long): Long = a * b
    def decShiftLeft(a: Long, ones: Int): Long = (a * 10) + ones
    val one: Long = 1
    val minusOne: Long = -1
    val zero: Long = 0
  }


  def apply(bb: ByteBuf): ParserFn = new Parser(bb)
  def apply(bytes: Array[Byte]): ParserFn = new Parser(Unpooled.copiedBuffer(bytes))
  def apply(string: String): ParserFn = new Parser(Unpooled.copiedBuffer(string, Utf8))

  def foreach(bb: ByteBuf)(f: RespHandler): Option[NotEnoughData] = {
    val parser = Parser(bb)
    @tailrec def loop(): Option[NotEnoughData] =
      if (!bb.isReadable) None
      else parser() match {
        case ned: NotEnoughData => Some(ned)
        case x => {
          f(x)
          loop()
        }
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

  private def notEnoughData() =
    NotEnoughData(bb.resetReaderIndex())

  private def unknownType() =
    ProtocolError(bb.resetReaderIndex(), typeChars)

  private def expected(expected: Byte) =
    ProtocolError(bb, List(expected))

  private def peek =
    bb.getByte(bb.readerIndex())

  private def read() =
    bb.readByte()

  private def skip() =
    bb.readerIndex(bb.readerIndex() + 1)

  private def requireLen(len: Int) =
    bb.isReadable(len)

  private def read(b: Byte): Option[ErrorType] = {
    if (!bb.isReadable) Some(notEnoughData())
    else if (peek != b) Some(expected(b))
    else {
      skip()
      None
    }
  }

  private def andRequireCrLf(value: RespType) =
    read(Cr).orElse(read(Lf)).getOrElse(value)

  private def parseLen() =
    parseNum[Int]

  private def parseInteger() = parseNum[Long] match {
    case Left(e) => e
    case Right(l) => RespInteger(l)
  }

  @tailrec
  private def parseNum[@specialized(Int, Long) A](n: A, neg: A)(implicit A: Num[A]): Either[ErrorType, A] = {
    if (!bb.isReadable) {
      Left(notEnoughData())
    } else {
      val current = read()
      current match {
        case Cr => read(Lf).toLeft(A.times(n, neg))
        case Minus => parseNum(n, A.minusOne)
        case b => parseNum(A.decShiftLeft(n, b - '0'), neg)
      }
    }
  }
  private def parseNum[@specialized(Int, Long) A](implicit A: Num[A]): Either[ErrorType, A] = parseNum(A.zero, A.one)

  private def readStringOfLen(len: Int)(ct: ByteBuf => DataType) = {
    if (!requireLen(len)) notEnoughData()
    else andRequireCrLf(ct(bb.readBytes(len)))
  }

  private def parseSimpleString() = {
    val len = bb.bytesBefore(Cr)
    if (len == -1) notEnoughData()
    else readStringOfLen(len)(b => RespString(b.toString(Utf8)))
  }

  private def parseError() = {
    val len = bb.bytesBefore(Cr)
    if (len == -1) notEnoughData()
    else readStringOfLen(len)(b => RespError(b.toString(Utf8)))
  }

  private def parseBulkString() = parseLen() match {
    case Left(ned) => ned
    case Right(len) => {
      if (len == -1) NullString
      else readStringOfLen(len)(b => RespBytes(b))
    }
  }

  private def parseArray() = parseLen() match {
    case Left(ned) => ned
    case Right(size) => {
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
  }

  private def quickApply(): RespType = {
    if (!bb.isReadable) notEnoughData()
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

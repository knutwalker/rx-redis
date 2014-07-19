package rx.redis.api

import io.netty.buffer.{ByteBuf, ByteBufAllocator, PooledByteBufAllocator, UnpooledByteBufAllocator}
import io.reactivex.netty.channel.ContentTransformer

import java.nio.charset.{StandardCharsets, Charset}
import scala.annotation.implicitNotFound


@implicitNotFound("No type class found for ${A}. You have to implement an rx.redis.api.Write[${A}] in order to send ${A} directly.")
trait Writes[A] {

  def write(value: A, allocator: ByteBufAllocator): ByteBuf

  def write(value: A): ByteBuf =
    write(value, Writes.unpooled)

  lazy val contentTransformer: ContentTransformer[A] =
    new ContentTransformer[A] {
      def call(t1: A, t2: ByteBufAllocator): ByteBuf = write(t1, t2)
    }
}

object Writes extends HigherWrites {
  private[redis] final val unpooled = UnpooledByteBufAllocator.DEFAULT
  private[redis] final val pooled = PooledByteBufAllocator.DEFAULT

  @inline def apply[T](implicit T: Writes[T]): Writes[T] = T

  implicit object DefaultStringWrites extends Writes[String] {
    private final val charset = Charset.defaultCharset

    def write(value: String, allocator: ByteBufAllocator): ByteBuf = {
      val contentAsBytes: Array[Byte] = value.getBytes(charset)
      allocator.buffer(contentAsBytes.length).writeBytes(contentAsBytes)
    }
  }

  implicit object ByteArrayWrites extends Writes[Array[Byte]] {
    def write(value: Array[Byte], allocator: ByteBufAllocator): ByteBuf = {
      allocator.buffer(value.length).writeBytes(value)
    }
  }
}

trait HigherWrites {
  import rx.redis.api.HigherWrites._

  abstract class ZeroArgsWrites[A] extends Writes[A] {
    def name: String

    final def write(value: A, allocator: ByteBufAllocator): ByteBuf =
      bytes(bytes(name, allocator), Nil, allocator)
  }

  abstract class ArgsWrites1[T, A: Writes] extends Writes[T] {
    def name: String
    def arg(value: T): A

    final def write(value: T, allocator: ByteBufAllocator): ByteBuf = {
      val cmd = bytes(name, allocator)
      val args = List(bytes(arg(value), allocator))
      bytes(cmd, args, allocator)
    }
  }

  abstract class ArgsWrites2[T, A: Writes, B: Writes] extends Writes[T] {
    def name: String
    def args(value: T): (A, B)

    final def write(value: T, allocator: ByteBufAllocator): ByteBuf = {
      val cmd = bytes(name, allocator)
      val (a1, a2) = args(value)
      val as = List(
        bytes(a1, allocator),
        bytes(a2, allocator)
      )
      bytes(cmd, as, allocator)
    }
  }

  abstract class ArgsWrites3[T, A: Writes, B: Writes, C: Writes] extends Writes[T] {
    def name: String
    def args(value: T): (A, B, C)

    final def write(value: T, allocator: ByteBufAllocator): ByteBuf = {
      val cmd = bytes(name, allocator)
      val (a1, a2, a3) = args(value)
      val as = List(
        bytes(a1, allocator),
        bytes(a2, allocator),
        bytes(a3, allocator)
      )
      bytes(cmd, as, allocator)
    }
  }

  abstract class ArgsWrites4[T, A: Writes, B: Writes, C: Writes, D: Writes] extends Writes[T] {
    def name: String
    def args(value: T): (A, B, C, D)

    final def write(value: T, allocator: ByteBufAllocator): ByteBuf = {
      val cmd = bytes(name, allocator)
      val (a1, a2, a3, a4) = args(value)
      val as = List(
        bytes(a1, allocator),
        bytes(a2, allocator),
        bytes(a3, allocator),
        bytes(a4, allocator)
      )
      bytes(cmd, as, allocator)
    }
  }

  abstract class ArgsWrites5[T, A: Writes, B: Writes, C: Writes, D: Writes, E: Writes] extends Writes[T] {
    def name: String
    def args(value: T): (A, B, C, D, E)

    final def write(value: T, allocator: ByteBufAllocator): ByteBuf = {
      val cmd = bytes(name, allocator)
      val (a1, a2, a3, a4, a5) = args(value)
      val as = List(
        bytes(a1, allocator),
        bytes(a2, allocator),
        bytes(a3, allocator),
        bytes(a4, allocator),
        bytes(a5, allocator)
      )
      bytes(cmd, as, allocator)
    }
  }

  abstract class ArgsWrites6[T, A: Writes, B: Writes, C: Writes, D: Writes, E: Writes, F: Writes] extends Writes[T] {
    def name: String
    def args(value: T): (A, B, C, D, E, F)

    final def write(value: T, allocator: ByteBufAllocator): ByteBuf = {
      val cmd = bytes(name, allocator)
      val (a1, a2, a3, a4, a5, a6) = args(value)
      val as = List(
        bytes(a1, allocator),
        bytes(a2, allocator),
        bytes(a3, allocator),
        bytes(a4, allocator),
        bytes(a5, allocator),
        bytes(a6, allocator)
      )
      bytes(cmd, as, allocator)
    }
  }

  abstract class ArgsWrites7[T, A: Writes, B: Writes, C: Writes, D: Writes, E: Writes, F: Writes, G: Writes] extends Writes[T] {
    def name: String
    def args(value: T): (A, B, C, D, E, F, G)

    final def write(value: T, allocator: ByteBufAllocator): ByteBuf = {
      val cmd = bytes(name, allocator)
      val (a1, a2, a3, a4, a5, a6, a7) = args(value)
      val as = List(
        bytes(a1, allocator),
        bytes(a2, allocator),
        bytes(a3, allocator),
        bytes(a4, allocator),
        bytes(a5, allocator),
        bytes(a6, allocator),
        bytes(a7, allocator)
      )
      bytes(cmd, as, allocator)
    }
  }
}
private[redis] object HigherWrites {
  private final val StringMarker = '$'.toByte
  private final val ArrayMarker = '*'.toByte
  private final val Separator = Array('\r'.toByte, '\n'.toByte)

  private def arrayCapacity(size: Int) = 4 + (size / 10)
  private def bulkCapacity(len: Int) = 6 + (len / 10) + len

  def bytes[A](a: A, allocator: ByteBufAllocator)(implicit A: Writes[A]): ByteBuf =
    A.write(a, allocator)

  def bytes(name: ByteBuf, args: Seq[ByteBuf], allocator: ByteBufAllocator): ByteBuf = {
    val size = args.size + 1
    val nameLen = name.readableBytes()

    val capacity =
      arrayCapacity(size) +
      bulkCapacity(nameLen) +
      args.foldLeft(0)((s, a) => s + bulkCapacity(a.readableBytes()))

    val buf = allocator.buffer(capacity)
      .writeByte(ArrayMarker)
      .writeBytes(size.toString.getBytes)
      .writeBytes(Separator)
      .writeByte(StringMarker)
      .writeBytes(nameLen.toString.getBytes)
      .writeBytes(Separator)
      .writeBytes(name)
      .writeBytes(Separator)

    args foreach { a =>
      buf.writeByte(StringMarker)
        .writeBytes(a.readableBytes().toString.getBytes)
        .writeBytes(Separator)
        .writeBytes(a)
        .writeBytes(Separator)
    }

    println(buf.toString(StandardCharsets.UTF_8).replaceAllLiterally("\r\n", "\\r\\n"))

    buf
  }
}

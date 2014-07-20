package rx.redis.serialization

import io.netty.buffer.{ByteBuf, ByteBufAllocator}
import io.reactivex.netty.channel.ContentTransformer

import java.nio.charset.Charset
import scala.annotation.{implicitNotFound, tailrec}
import scala.collection.mutable.ArrayBuffer
import scala.language.experimental.macros


@implicitNotFound("No type class found for ${A}. You have to implement an rx.redis.api.Write[${A}] in order to send ${A} directly.")
trait Writes[A] {

  def write(value: A, allocator: ByteBufAllocator): ByteBuf

  lazy val contentTransformer: ContentTransformer[A] =
    new ContentTransformer[A] {
      def call(t1: A, t2: ByteBufAllocator): ByteBuf = write(t1, t2)
    }

  protected def int2bytes(n: Int): Array[Byte] =
    Writes.long2bytes(n)
}

object Writes {

  @inline def apply[T](implicit T: Writes[T]): Writes[T] = T

  def writes[A]: Writes[A] = macro Macros.writes[A]

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

  implicit object LongWrites extends Writes[Long] {
    def write(value: Long, allocator: ByteBufAllocator): ByteBuf = {
      ByteArrayWrites.write(long2bytes(value), allocator)
    }
  }

//  implicit object ByteBufWrites extends Writes[ByteBuf] {
//    def write(value: ByteBuf, allocator: ByteBufAllocator): ByteBuf = value
//  }

  //  implicit def SeqWrites[A: Writes]: Writes[Seq[A]] = new Writes[Seq[A]] {
  //    def write(value: Seq[A], allocator: ByteBufAllocator): ByteBuf = {
  //      val buf = allocator.buffer()
  //      val tci = Writes[A]
  //      for (v <- value) {
  //        buf.writeBytes(tci.write(v, allocator))
  //      }
  //      buf
  //    }
  //  }

  private def long2bytes(n: Long): Array[Byte] = {
    @tailrec
    def loop(l: Long, b: ArrayBuffer[Byte]): Array[Byte] = {
      if (l == 0) b.reverse.toArray
      else {
        b += (l % 10 + '0').toByte
        loop(l / 10, b)
      }
    }
    if (n == 0) Array((0 + '0').toByte)
    else loop(n, new ArrayBuffer[Byte](10))
  }

  private val ArrayMarker = '*'.toByte
  private val StringMarker = '$'.toByte
  private val Cr = '\r'.toByte
  private val Lf = '\n'.toByte

  /* Gets implements by macro generation */
  private[redis] trait MWrites[A] extends Writes[A] {
    def nameHeader: Array[Byte]
    def sizeHint(value: A): Long
    def writeArgs(buf: ByteBuf, value: A): Unit

    protected def writeArg[B](buf: ByteBuf, value: B, tci: Writes[B]): Unit = {
      val contentBytes = tci.write(value, buf.alloc())
      val contentLength = long2bytes(contentBytes.readableBytes())
      buf.
        writeByte(StringMarker).
        writeBytes(contentLength).
        writeByte(Cr).writeByte(Lf).
        writeBytes(contentBytes).
        writeByte(Cr).writeByte(Lf)
    }

    protected def writeHeader(buf: ByteBuf, value: A): Unit = {
      buf.
        writeByte(ArrayMarker).
        writeBytes(long2bytes(sizeHint(value))).
        writeByte(Cr).writeByte(Lf).
        writeBytes(nameHeader)
    }

    final def write(value: A, allocator: ByteBufAllocator): ByteBuf = {
      val buf = allocator.buffer()
      writeHeader(buf, value)
      writeArgs(buf, value)
      buf
    }
  }
}

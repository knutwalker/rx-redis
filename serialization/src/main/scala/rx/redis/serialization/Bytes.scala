package rx.redis.serialization


import rx.redis.util.Utf8

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.concurrent.TimeUnit
import scala.annotation.{implicitNotFound, tailrec}
import scala.collection.mutable
import scala.concurrent.duration.{Deadline, FiniteDuration}


@implicitNotFound("No type class found for ${A}. You have to implement an rx.redis.serialization.Bytes[${A}] in order to use ${A} as custom value.")
trait Bytes[A] {

  def bytes(value: A): Array[Byte]

  def value(bytes: Array[Byte]): A
}

object Bytes {

  @inline def apply[T](implicit T: Bytes[T]): Bytes[T] = T

  def from[A: Bytes, B](f: B => A)(g: A => B): Bytes[B] =
    new Bytes[B] {
      def bytes(value: B): Array[Byte] =
        Bytes[A].bytes(f(value))

      def value(bytes: Array[Byte]): B =
        g(Bytes[A].value(bytes))
    }

  implicit object ByteArrayBytes extends Bytes[Array[Byte]] {
    def bytes(value: Array[Byte]): Array[Byte] = value

    def value(bytes: Array[Byte]): Array[Byte] = bytes
  }

  implicit def IterableBytes[A: Bytes]: Bytes[Iterable[A]] = new Bytes[Iterable[A]] {

    private def writeInt(b: ByteArrayOutputStream, n: Int) = {
      val buf = new Array[Byte](4)
      buf(0) = (n >>> 24).asInstanceOf[Byte]
      buf(1) = (n >>> 16).asInstanceOf[Byte]
      buf(2) = (n >>> 8).asInstanceOf[Byte]
      buf(3) = n.asInstanceOf[Byte]
      b.write(buf)
    }

    private def readInt(b: ByteArrayInputStream): Int = {
      val buf = new Array[Byte](4)
      val bytesRead = b.read(buf)
      if (bytesRead == 4) {
        (buf(0) & 0xff) << 24 |
        (buf(1) & 0xff) << 16 |
        (buf(2) & 0xff) << 8 |
        buf(3) & 0xff
      } else {
        0
      }
    }

    private def readA(b: ByteArrayInputStream, tc: Bytes[A]): A = {
      val length = readInt(b)
      val buf = new Array[Byte](length)
      val bytesRead = b.read(buf)
      if (bytesRead == length) tc.value(buf) else null.asInstanceOf[A]
    }

    def bytes(value: Iterable[A]): Array[Byte] = {
      val it = value.iterator
      if (!it.hasNext) Array.empty[Byte]
      else {
        val baos = new ByteArrayOutputStream()
        writeInt(baos, value.size)
        val tci = Bytes[A]
        while(it.hasNext) {
          val item = tci.bytes(it.next())
          writeInt(baos, item.length)
          baos.write(item)
        }
        baos.close()
        baos.toByteArray
      }
    }

    def value(bytes: Array[Byte]): Iterable[A] = {
      val bais = new ByteArrayInputStream(bytes)
      val size = readInt(bais)
      val buf = new mutable.ListBuffer[A]
      buf.sizeHint(size)
      val tc = Bytes[A]
      @tailrec
      def loop(n: Int): Iterable[A] =
        if (n == 0) buf.result()
        else {
          val item = readA(bais, tc)
          if (item == null) buf.result()
          else {
            buf += item
            loop(n - 1)
          }
        }
      loop(size)
    }
  }

  implicit val StringBytes = from((_: String).getBytes(Utf8))(b => new String(b, Utf8))

  implicit val LongBytes = from((_: Long).toString)(a => a.toLong)

  implicit val DurationBytes = from((_: FiniteDuration).toSeconds)(FiniteDuration(_, TimeUnit.SECONDS))

  implicit val DeadlineBytes = from((d: Deadline) =>
    (d.timeLeft.toMillis + System.currentTimeMillis()) / 1000)(t =>
    FiniteDuration(t * 1000 - System.currentTimeMillis(), TimeUnit.MILLISECONDS).fromNow)
}

package rx.redis.serialization

import rx.redis.resp.{DataType, RespArray, RespBytes}

import scala.annotation.implicitNotFound
import scala.collection.mutable.ListBuffer
import scala.language.experimental.macros


@implicitNotFound("No type class found for ${A}. You have to implement an rx.redis.api.Write[${A}] in order to send ${A} directly.")
trait Writes[A] {

  def write(value: A): DataType
}

object Writes {
  @inline def apply[A](implicit A: Writes[A]): Writes[A] = A

  def writes[A]: Writes[A] = macro Macros.writes[A]
  
  implicit object DirectStringWrites extends Writes[String] {
    def write(value: String): DataType = {
      val items: List[RespBytes] = value.split(' ').map(RespBytes(_))(collection.breakOut)
      RespArray(items)
    }
  }

  /* implemented by macro generation */
  private[serialization] trait MWrites[A] extends Writes[A] {
    def nameHeader: Array[Byte]
    def sizeHint(value: A): Long
    def writeArgs(buf: ListBuffer[DataType], value: A): Unit

    protected def writeArg[B](buf: ListBuffer[DataType], value: B, B: Bytes[B]): Unit = {
      buf += RespBytes(B.bytes(value))
    }

    private def writeHeader(buf: ListBuffer[DataType]): Unit = {
      buf += RespBytes(nameHeader)
    }

    final def write(value: A): DataType = {
      val buf = new ListBuffer[DataType]
      writeHeader(buf)
      writeArgs(buf, value)
      RespArray(buf.result())
    }
  }
}

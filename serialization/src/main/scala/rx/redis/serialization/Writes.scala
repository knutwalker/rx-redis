package rx.redis.serialization

import rx.redis.resp.{DataType, RespArray, RespBytes}

import scala.annotation.implicitNotFound
import scala.language.experimental.macros


@implicitNotFound("No type class found for ${A}. You have to implement an rx.redis.serialization.Writes[${A}] in order to send ${A} directly.")
trait Writes[A] {

  def write(value: A): DataType
}

object Writes {
  @inline def apply[A](implicit A: Writes[A]): Writes[A] = A

  def writes[A]: Writes[A] = macro Macros.writes[A]
  
  implicit object DirectStringWrites extends Writes[String] {
    def write(value: String): DataType = {
      val items: Array[DataType] = value.split(' ').map(RespBytes(_))(collection.breakOut)
      RespArray(items)
    }
  }
}

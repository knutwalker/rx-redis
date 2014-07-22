package rx.redis.serialization

import rx.Observable
import rx.functions.Func1

import rx.redis.resp.{RespArray, RespBytes, RespInteger, RespString, RespType}

import scala.collection.JavaConverters._



trait Reads[A] { self =>

  def pf: PartialFunction[RespType, A]

  val obs: Func1[_ >: RespType, _ <: Observable[A]] =
    new Func1[RespType, Observable[A]] {
      def call(t1: RespType): Observable[A] = {
        if (self.pf.isDefinedAt(t1))
          Observable.just(self.pf(t1))
        else
          Observable.empty()
      }
    }

  def obsT[B](implicit B: Bytes[B], ev: A =:= Array[Byte]): Func1[_ >: RespType, _ <: Observable[B]] =
    new Func1[RespType, Observable[B]] {
      def call(t1: RespType): Observable[B] = {
        if (self.pf.isDefinedAt(t1))
          Observable.just(B.value(self.pf(t1)))
        else
          Observable.empty()
      }
    }

  def obsOptT[B](implicit B: Bytes[B], ev: A =:= Array[Byte]): Func1[_ >: RespType, _ <: Observable[Option[B]]] =
    new Func1[RespType, Observable[Option[B]]] {
      def call(t1: RespType): Observable[Option[B]] = {
        if (self.pf.isDefinedAt(t1))
          Observable.just(Some(B.value(self.pf(t1))))
        else
          Observable.just(None)
      }
    }

  def obsMT[B](implicit B: Bytes[B], ev: A <:< List[RespType]): Func1[_ >: RespType, _ <: Observable[B]] =
    new Func1[RespType, Observable[B]] {
      def call(t1: RespType): Observable[B] = {
        if (self.pf.isDefinedAt(t1))
          Observable.from(self.pf(t1).collect(Reads.bytes.pf).map(B.value).asJava)
        else
          Observable.empty()
      }
    }

  def obsMTOpt[B](implicit B: Bytes[B], ev: A <:< List[RespType]): Func1[_ >: RespType, _ <: Observable[Option[B]]] =
    new Func1[RespType, Observable[Option[B]]] {
      def call(t1: RespType): Observable[Option[B]] = {
        if (self.pf.isDefinedAt(t1))
          Observable.from(self.pf(t1).map(Reads.bytes.pf.andThen(B.value).lift).asJava)
        else
          Observable.empty()
      }
    }

  def obsAB[B, C](implicit B: Bytes[B], C: Bytes[C], ev: A <:< List[(RespType, RespType)]): Func1[_ >: RespType, _ <: Observable[(B, C)]] =
    new Func1[RespType, Observable[(B, C)]] {
      def call(t1: RespType): Observable[(B, C)] = {
        if (self.pf.isDefinedAt(t1))
          Observable.from(self.pf(t1).collect{
            case (x, y) if Reads.bytes.pf.isDefinedAt(x) && Reads.bytes.pf.isDefinedAt(y) =>
              (B.value(Reads.bytes.pf(x)), C.value(Reads.bytes.pf(y)))
          }.asJava)
        else
          Observable.empty()
      }
    }
}

object Reads {
  @inline def apply[A](implicit A: Reads[A]): Reads[A] = A

  implicit val bool: Reads[Boolean] = new Reads[Boolean] {
    val pf: PartialFunction[RespType, Boolean] = {
      case RespString(s) if s == "OK" => true
      case RespInteger(i) => i > 0
    }
  }

  implicit val bytes: Reads[Array[Byte]] = new Reads[Array[Byte]] {
    val pf: PartialFunction[RespType, Array[Byte]] = {
      case RespBytes(b) => b
      case RespString(s) => Bytes[String].bytes(s)
    }
  }

  implicit val int: Reads[Long] = new Reads[Long] {
    val pf: PartialFunction[RespType, Long] = {
      case RespInteger(l) => l
    }
  }

  implicit val list: Reads[List[RespType]] = new Reads[List[RespType]] {
    val pf: PartialFunction[RespType, List[RespType]] = {
      case RespArray(items) => items.toList
    }
  }

  implicit val unzip: Reads[List[(RespType, RespType)]] = new Reads[List[(RespType, RespType)]] {
    val pf: PartialFunction[RespType, List[(RespType, RespType)]] = {
      case RespArray(items) =>
        items.grouped(2).map(xs => (xs(0), xs(1))).toList
    }
  }
}

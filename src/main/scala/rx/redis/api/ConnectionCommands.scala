package rx.redis.api

import rx.Observable

import rx.redis.commands.{Echo, Ping}
import rx.redis.serialization.{Bytes, Reads}


trait ConnectionCommands { this: Client =>
  def ping(): Observable[String] =
    command(Ping).flatMap(Reads.bytes.obsT[String])

  def echo[A](msg: A)(implicit A: Bytes[A]): Observable[A] =
    command(Echo(msg)).flatMap(Reads.bytes.obsT[A])
}

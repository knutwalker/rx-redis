package rx.redis.api

import io.netty.buffer.ByteBuf
import rx.lang.scala.Observable


trait Client[A] {
  def command(cmd: ByteBuf): Observable[A]
  def command(cmd: Array[Byte]): Observable[A]
  def command(cmd: String): Observable[A]

  def shutdown(): Observable[Unit]
  def closedObservable: Observable[Unit]
}

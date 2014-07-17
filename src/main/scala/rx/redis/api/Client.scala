package rx.redis.api

import scala.concurrent.Future

import io.netty.buffer.ByteBuf
import rx.lang.scala.Observable
import rx.redis.resp.RespType

trait Client[A] {
  def command(cmd: ByteBuf): Observable[A]
  def command(cmd: Array[Byte]): Observable[A]
  def command(cmd: String): Observable[A]

  def shutdown(): Future[Unit]
  def closeFuture: Future[Unit]
}

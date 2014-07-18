package rx.redis.api

import io.netty.buffer.ByteBuf
import rx.lang.scala.Observable

import rx.redis.resp.RespType


trait Client
extends StringCommands {

  def command(cmd: ByteBuf): Observable[RespType]
  def command[A](cmd: A)(implicit B: Writes[A]): Observable[RespType]

  def shutdown(): Observable[Unit]
  def closedObservable: Observable[Unit]
}

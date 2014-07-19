package rx.redis.api

import io.netty.buffer.ByteBuf
import rx.Observable

import rx.redis.resp.RespType
import rx.redis.serialization.Writes


trait Client
extends StringCommands
with ConnectionCommands {

  def command(cmd: ByteBuf): Observable[RespType]
  def command[A](cmd: A)(implicit A: Writes[A]): Observable[RespType]

  def shutdown(): Observable[Unit]
  def closedObservable: Observable[Unit]
}

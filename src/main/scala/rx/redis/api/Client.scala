package rx.redis.api

import io.netty.buffer.ByteBuf
import rx.lang.scala.Observable
import rx.redis.resp.RespType

trait Client {
  def command(cmd: ByteBuf): Observable[RespType]
  def command(cmd: Array[Byte]): Observable[RespType]
  def command(cmd: String): Observable[RespType]

  def shutdown(): Unit
  def await(): Unit
}

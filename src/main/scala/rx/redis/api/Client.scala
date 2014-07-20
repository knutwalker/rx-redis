package rx.redis.api

import rx.Observable

import rx.redis.resp.RespType
import rx.redis.serialization.Writes


trait Client
extends StringCommands
with ConnectionCommands
with HashCommands
with KeyCommands
{

  def command[A](cmd: A)(implicit A: Writes[A]): Observable[RespType]

  def shutdown(): Observable[Unit]
  def closedObservable: Observable[Unit]
}

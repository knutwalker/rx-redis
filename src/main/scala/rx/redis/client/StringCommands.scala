package rx.redis.client

import rx.lang.scala.Observable

import rx.redis.api
import rx.redis.api.Writes
import rx.redis.commands.{Get, Set}
import rx.redis.resp.RespType


private[redis] trait StringCommands extends api.StringCommands { this: RxRedisClient =>
  def get(key: String): Observable[RespType] = {
    command(Get.GetWrites.toBytes(key, allocator))
  }
  def set[A: Writes](key: String, value: A): Observable[RespType] = {
    command(Set.SetWrites.toBytes(key, Writes[A].toBytes(value, allocator), allocator))
  }
}

package rx.redis.client

import rx.lang.scala.Observable
import rx.redis.api
import rx.redis.api.Write
import rx.redis.commands.{Get, Set}
import rx.redis.resp.RespType


private[redis] trait StringCommands extends api.StringCommands { this: RxRedisClient =>
  def get(key: String): Observable[RespType] = {
    command(Get.GetWrite.toBytes(key, allocator))
  }
  def set[A](key: String, value: A)(implicit A: Write[A]): Observable[RespType] = {
    command(Set.SetWrite.toBytes(key, A.toBytes(value), allocator))
  }
}

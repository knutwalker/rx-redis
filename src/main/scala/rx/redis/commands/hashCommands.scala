package rx.redis.commands

import rx.redis.serialization.Writes


case class HGet(key: String, field: String)
object HGet {
  implicit val HGetWrites = Writes.writes[HGet]
}

case class HGetAll(key: String)
object HGetAll {
  implicit val HGetAllWrites = Writes.writes[HGetAll]
}

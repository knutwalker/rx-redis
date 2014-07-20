package rx.redis.commands

import rx.redis.serialization.Writes


case class Get(key: String)
object Get {
  implicit val GetWrites: Writes[Get] = Writes.writes[Get]
}


case class Set[A: Writes](key: String, value: A)
object Set {
  implicit def SetWrites[A: Writes]: Writes[Set[A]] = Writes.writes[Set[A]]
}


case class Incr(key: String)
object Incr {
  implicit val IncrWrites: Writes[Incr] = Writes.writes[Incr]
}


case class IncrBy(key: String, amount: Long)
object IncrBy {
  implicit val IncrByWrites: Writes[IncrBy] = Writes.writes[IncrBy]
}


case class MGet(keys: String*)
object MGet {
  implicit val MGetWrites: Writes[MGet] = Writes.writes[MGet]
}

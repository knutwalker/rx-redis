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

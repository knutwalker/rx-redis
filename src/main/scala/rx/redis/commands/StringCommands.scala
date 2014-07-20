package rx.redis.commands

import rx.redis.serialization.{Bytes, Writes}

import scala.concurrent.duration.Duration


case class Get(key: String)
object Get {
  implicit val GetWrites: Writes[Get] = Writes.writes[Get]
}


case class Set[A: Bytes](key: String, value: A)
object Set {
  implicit def SetWrites[A: Bytes]: Writes[Set[A]] = Writes.writes[Set[A]]
}


case class SetEx[A: Bytes](key: String, expires: Duration, value: A)
object SetEx {
  implicit def SetExWrites[A: Bytes]: Writes[SetEx[A]] = Writes.writes[SetEx[A]]
}


case class SetNx[A: Bytes](key: String, value: A)
object SetNx {
  implicit def SetNxWrites[A: Bytes]: Writes[SetNx[A]] = Writes.writes[SetNx[A]]
}


case class Incr(key: String)
object Incr {
  implicit val IncrWrites: Writes[Incr] = Writes.writes[Incr]
}


case class Decr(key: String)
object Decr {
  implicit val DecrWrites: Writes[Decr] = Writes.writes[Decr]
}


case class IncrBy(key: String, amount: Long)
object IncrBy {
  implicit val IncrByWrites: Writes[IncrBy] = Writes.writes[IncrBy]
}


case class DecrBy(key: String, amount: Long)
object DecrBy {
  implicit val DecrByWrites: Writes[DecrBy] = Writes.writes[DecrBy]
}


case class MGet(keys: String*)
object MGet {
  implicit val MGetWrites: Writes[MGet] = Writes.writes[MGet]
}


case class MSet[A: Bytes](keys: (String, A)*)
object MSet {
  implicit def MSetWrites[A: Bytes]: Writes[MSet[A]] = Writes.writes[MSet[A]]
}


case class StrLen(key: String)
object StrLen {
  implicit val StrLenWrites: Writes[StrLen] = Writes.writes[StrLen]
}

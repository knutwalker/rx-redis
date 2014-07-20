package rx.redis.commands

import rx.redis.serialization.{Bytes, Writes}


case object Ping {
  implicit val PingWrites: Writes[Ping.type] = Writes.writes[Ping.type]
}

case class Echo[A: Bytes](value: A)
object Echo {
  implicit def EchoWrites[A: Bytes] = Writes.writes[Echo[A]]
}

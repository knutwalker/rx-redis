package rx.redis.commands

import rx.redis.serialization.Writes


case object Ping {
  implicit val PingWrites = Writes.writes[Ping.type]
}

case class Echo[A: Writes](value: A)
object Echo {
  implicit def echoWrites[A: Writes]: Writes[Echo[A]] = Writes.writes[Echo[A]]
}

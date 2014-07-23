package rx.redis.commands

import rx.redis.serialization.{BytesFormat, Writes}


case object Ping {
  implicit val PingWrites: Writes[Ping.type] = Writes.writes[Ping.type]
}

case class Echo[A: BytesFormat](value: A)
object Echo {
  implicit def EchoWrites[A: BytesFormat] = Writes.writes[Echo[A]]
}

package rx.redis.commands

import rx.redis.api.Writes
import rx.redis.api.Writes._


case object Ping {
  implicit object PingWrites extends ZeroArgsWrites[Ping.type] {
    val name: String = "PING"
  }
}

case class Echo[A: Writes](value: A)
object Echo {
  implicit def echoWrites[A: Writes] = new ArgsWrites1[Echo[A], A] {
    def name: String = "ECHO"
    def arg(value: Echo[A]): A = value.value
  }
}

package rx.redis.commands

import rx.redis.api.Writes
import rx.redis.api.Writes._


case class Get(key: String)
object Get {
  implicit object GetWrites extends ArgsWrites1[Get, String] {
    val name: String = "GET"
    def arg(value: Get): String = value.key
  }
}


case class Set[A: Writes](key: String, value: A)
object Set {
  implicit def setWrites[A: Writes] = new ArgsWrites2[Set[A], String, A] {
    val name: String = "SET"
    def args(value: Set[A]): (String, A) = (value.key, value.value)
  }
}

package rx.redis.commands

import org.scalatest.FunSuite

import rx.redis.serialization.Writes

import java.nio.charset.StandardCharsets


class CommandsSuite extends FunSuite {

  val charset = StandardCharsets.UTF_8

  protected def pretty(s: String, snip: Option[Int] = None) =
    Some(s.replaceAllLiterally("\r\n", "\\r\\n")).map(s => snip.fold(s)(s.take)).get

  protected def cmd[A: Writes](c: A, expected: String) = {
    val buf = Writes[A].write(c)
    assert(buf.toString === expected)
  }

  protected def cmds[A: Writes](c: A, expectedParts: String*) =
    cmd(c, expectedParts.mkString("", "\r\n", "\r\n"))
}

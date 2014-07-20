package rx.redis.commands

import io.netty.buffer.{ByteBuf, UnpooledByteBufAllocator}

import org.scalatest.FunSuite

import rx.redis.serialization.Writes

import java.nio.charset.StandardCharsets


class CommandsSuite extends FunSuite {

  val charset = StandardCharsets.UTF_8

  val alloc = UnpooledByteBufAllocator.DEFAULT

  protected def bb(b: ByteBuf): String =
    b.toString(charset)

  protected def pretty(s: String, snip: Option[Int] = None) =
    Some(s.replaceAllLiterally("\r\n", "\\r\\n")).map(s => snip.fold(s)(s.take)).get

  protected def cmd[A: Writes](c: A, expected: String) = {
    val buf = Writes[A].write(c, alloc)
    try {
      assert(bb(buf) === expected)
    } finally buf.release()
  }

  protected def cmds[A: Writes](c: A, expectedParts: String*) =
    cmd(c, expectedParts.mkString("", "\r\n", "\r\n"))
}

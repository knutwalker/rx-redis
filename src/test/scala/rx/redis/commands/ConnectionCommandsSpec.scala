package rx.redis.commands

import rx.redis.util._

class ConnectionCommandsSpec extends CommandsSuite {

  test("PING") {
    val ping = Ping
    cmds(ping, "PING")
    cmd(ping, resp"PING")
  }

  test("ECHO") {
    val echo = Echo("foobar")
    cmds(echo, "ECHO", "foobar")
    cmd(echo, resp"ECHO foobar")
  }
}

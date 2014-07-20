package rx.redis.commands

import rx.redis.util._

class ConnectionCommandsSpec extends CommandsSuite {

  test("PING") {
    val ping = Ping
    cmds(ping, "*1", "$4", "PING")
    cmd(ping, resp"PING")
  }

  test("ECHO") {
    val echo = Echo("foobar")
    cmds(echo, "*2", "$4", "ECHO", "$6", "foobar")
    cmd(echo, resp"ECHO foobar")
  }
}

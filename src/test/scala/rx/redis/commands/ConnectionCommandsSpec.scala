package rx.redis.commands

import rx.redis._

class ConnectionCommandsSpec extends CommandsSuite {

  test("PING") {
    val ping = Ping
    sers(ping, "PING")
    ser(ping, cmd"PING")
  }

  test("ECHO") {
    val echo = Echo("foobar")
    sers(echo, "ECHO", "foobar")
    ser(echo, cmd"ECHO foobar")
  }
}

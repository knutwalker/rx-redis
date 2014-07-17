package rx.redis


object TestTcp extends App {

  private val INFO = "*1\r\n$4\r\nINFO\r\n"
  private val PING = "*1\r\n$4\r\nPING\r\n"
  private val INFO_SERVER = "*2\r\n$4\r\nINFO\r\n$6\r\nSERVER\r\n"

  val client = RxRedis("localhost", 6379)
  client.command(INFO).foreach(r => println("INFO: " + r))
  client.command(PING).foreach(r => println("PING: " + r))
  client.command(INFO_SERVER).foreach(r => {
    println("SERVER: " + r)
    client.shutdown()
  })
  client.await()
}

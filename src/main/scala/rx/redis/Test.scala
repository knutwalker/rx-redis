package rx.redis

import rx.redis.util.{command, preview}


object Test extends App {

  val client = RxRedis("localhost", 6379)

  val PING = command("PING")
  val INFO = command("INFO")
  val CLIENT_LIST = command("CLIENT LIST")
  val SERVER_INFO = command("INFO SERVER")

  client.command(PING).toBlocking.foreach { r =>
    println(s"first PING : ${preview(r)}")
  }

  println("after first PING")

  client.command(PING).toBlocking.foreach { r =>
    println(s"second PING : ${preview(r)}")
  }

  println("after second PING")

  client.command(INFO).foreach { r =>
    println(s"INFO : ${preview(r)}")
  }

  println("after INFO")

  client.command(PING).foreach { r =>
    println(s"third PING : ${preview(r)}")
  }

  println("after third PING")

  client.command(CLIENT_LIST).foreach { r =>
    println(s"CLIENT LIST : ${preview(r)}")
  }

  println("after CLIENT LIST")

  client.command(SERVER_INFO).foreach { r =>
    println(s"SERVER INFO: ${preview(r)}")
    client.shutdown()
  }

  println("after SERVER INFO")

  RxRedis.await(client)

  println("Client is closed")
}

package rx.redis

import rx.redis.util.command


object Test extends App {

  val client = RxRedis("localhost", 6379)

  val PING = command("PING")
  val INFO = command("INFO")
  val CLIENT_LIST = command("CLIENT LIST")
  val SERVER_INFO = command("INFO SERVER")


  client.command(PING).toBlocking.foreach { r =>
    println(s"first PING : $r")
  }

  println("after first PING")

  client.command(PING).toBlocking.foreach { r =>
    println(s"second PING : $r")
  }

  println("after second PING")

  client.command(INFO).foreach { r =>
    println(s"INFO : $r")
  }

  println("after INFO")

  client.command(PING).foreach { r =>
    println(s"third PING : $r")
  }

  println("after third PING")

  client.command(CLIENT_LIST).foreach { r =>
    println(s"CLIENT LIST : $r")
  }

  println("after CLIENT LIST")

  client.command(SERVER_INFO).foreach { r =>
    println(s"SERVER INFO: $r")
    client.shutdown()
  }

  println("after SERVER INFO")

  concurrent.Await.ready(client.closeFuture, concurrent.duration.Duration.Inf)

  println("Client is closed")
}

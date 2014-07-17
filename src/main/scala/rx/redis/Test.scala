package rx.redis

import rx.redis.util.command


object Test extends App {

  val client = RxRedis("localhost", 6379)

  client.command(command("INFO")).foreach(r => println("INFO: " + r))
  client.command(command("PING")).foreach(r => println("PING: " + r))
  client.command(command("CLIENT LIST")).foreach(r => println("CLIENT LIST: " + r))
  client.command(command("INFO SERVER")).foreach(r => {
    println("SERVER INFO: " + r)
    client.shutdown()
  })
  client.await()
}

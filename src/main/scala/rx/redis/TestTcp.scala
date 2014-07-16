package rx.redis


import java.nio.charset.StandardCharsets

import io.netty.buffer.{ByteBuf, Unpooled}
import io.reactivex.netty.RxNetty
import io.reactivex.netty.channel.ObservableConnection
import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable
import rx.redis.resp.RespType


object TestTcp extends App {

  private val client =
    RxNetty.newTcpClientBuilder("localhost", 6379)
      .defaultTcpOptions()
      .withName("Redis")
      .pipelineConfigurator(new RedisProtocolConfigurator())
      .build()

  private val connect = toScalaObservable(client.connect())

  private val utf_8 = StandardCharsets.UTF_8
  val info = Unpooled.copiedBuffer("*1\r\n$4\r\nINFO\r\n", utf_8)
  val clientList = Unpooled.copiedBuffer("*1\r\n$4\r\nPING\r\n", utf_8)
  val serverInfo = Unpooled.copiedBuffer("*2\r\n$4\r\nINFO\r\n$6\r\nSERVER\r\n", utf_8)

  def connectionHandler(connection: ObservableConnection[RespType, ByteBuf]): Observable[RespType] = {
    connection.writeAndFlush(info)
    connection.writeAndFlush(clientList)
    connection.writeAndFlush(serverInfo)
    connection.getInput
  }

  private val observable = connect.flatMap(connectionHandler)
  observable.take(3).toBlocking.foreach(println)

  client.shutdown()
}

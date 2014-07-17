package rx.redis

import java.util.concurrent.atomic.AtomicInteger

import io.netty.buffer.ByteBuf
import io.reactivex.netty.RxNetty
import io.reactivex.netty.client.RxClient
import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable
import rx.redis.pipeline.Configurator
import rx.redis.resp.RespType
import rx.redis.util.observers.DiscardingObserver

object RxRedis {
  def apply(host: String, port: Int): api.Client[RespType] = {
    val client =
      RxNetty.newTcpClientBuilder(host, port)
        .defaultTcpOptions()
        .withName("Redis")
        .pipelineConfigurator(new Configurator)
        .build()
    new RxRedis(client)
  }

  def await[A](client: api.Client[A]): Unit = {
    client.closedObservable.toBlocking.toList.lastOption
  }
}
final class RxRedis[A] private (client: RxClient[ByteBuf, A]) extends api.Client[A] {

  private val connect = toScalaObservable(client.connect().cache())
  private val elementsInFlight = new AtomicInteger(0)

  val responseStream = connect.flatMap(_.getInput)

  private def nextResponse(n: Int = 1) = {
    val inFlight = elementsInFlight.getAndIncrement
    responseStream.drop(inFlight).take(n)
      .doOnCompleted(elementsInFlight.decrementAndGet())
  }

  def command(cmd: ByteBuf): Observable[A] = {
    connect.foreach(_.writeAndFlush(cmd))
    nextResponse()
  }

  def command(cmd: Array[Byte]): Observable[A] = {
    connect.foreach(_.writeBytesAndFlush(cmd))
    nextResponse()
  }

  def command(cmd: String): Observable[A] = {
    connect.foreach(_.writeStringAndFlush(cmd))
    nextResponse()
  }

  def shutdown() = {
    val closeObs = connect.flatMap(_.close(true)) map (_ => ())
    closeObs.subscribe()
    client.shutdown()
    closeObs
  }

  lazy val closedObservable =
    DiscardingObserver(responseStream)
}

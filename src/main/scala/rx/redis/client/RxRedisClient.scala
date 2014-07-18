package rx.redis.client

import io.netty.buffer.ByteBuf
import io.reactivex.netty.client.RxClient
import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable

import rx.redis.api
import rx.redis.api.Writes
import rx.redis.resp.RespType
import rx.redis.util.observers.DiscardingObserver

import java.util.concurrent.atomic.AtomicInteger


private[redis] final class RxRedisClient (client: RxClient[ByteBuf, RespType])
  extends api.Client with StringCommands {

  private val connect = toScalaObservable(client.connect().cache())
  private val connection = connect.toBlocking.head

  private val elementsInFlight = new AtomicInteger(0)

  val responseStream = connection.getInput

  def allocator = connection.getAllocator

  private def nextResponse(n: Int = 1) = {
    val inFlight = elementsInFlight.getAndIncrement
    responseStream.drop(inFlight).take(n)
      .doOnCompleted(elementsInFlight.decrementAndGet())
  }

  def command(cmd: ByteBuf): Observable[RespType] = {
    connection.writeAndFlush(cmd)
    nextResponse()
  }

  def command[B](cmd: B)(implicit B: Writes[B]): Observable[RespType] = {
    connection.writeAndFlush(cmd, B.contentTransformer)
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

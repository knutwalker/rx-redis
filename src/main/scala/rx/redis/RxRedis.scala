package rx.redis

import io.netty.buffer.ByteBuf
import io.reactivex.netty.RxNetty
import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable
import rx.redis.pipeline.Configurator
import rx.redis.resp.RespType
import rx.redis.util.atom._

object RxRedis {
  def apply(host: String, port: Int): api.Client = new RxRedis(host, port)
}
final class RxRedis private (host: String, port: Int) extends api.Client {

  private val client =
    RxNetty.newTcpClientBuilder(host, port)
        .defaultTcpOptions()
        .withName("Redis")
        .pipelineConfigurator(new Configurator)
        .build()

  private val connect = toScalaObservable(client.connect().cache())
  private val allResponses = connect.flatMap(_.getInput)

  private val response = Atom(allResponses)
  private def nextResponse() = {
    response.swapAndReturn { r =>
      (r.drop(1), r.take(1))
    }
  }

  def command(cmd: ByteBuf): Observable[RespType] = {
    connect.foreach(_.writeAndFlush(cmd))
    nextResponse()
  }

  def command(cmd: Array[Byte]): Observable[RespType] = {
    connect.foreach(_.writeBytesAndFlush(cmd))
    nextResponse()
  }

  def command(cmd: String): Observable[RespType] = {
    connect.foreach(_.writeStringAndFlush(cmd))
    nextResponse()
  }

  def shutdown() = {
    connect.foreach(_.close(true))
    client.shutdown()
  }

  def await() = {
    allResponses.toBlocking.last
  }
}
